//! Which properties have a full-text index declared on them.
//!
//! Full-text search used to be a mapping from node to one arbitrary document,
//! handed over by hand. Nothing connected it to a property, which is why
//! `d.content @@ "..."` could match on text that had nothing to do with
//! `content`, and why a misspelled property name worked just as well.
//!
//! A declared index says: this property, on this label, holds text worth
//! searching. From there the engine keeps it current the same way it keeps an
//! equality index current, and `@@` means what it reads like.
//!
//! ## Sharing the index catalog
//!
//! Definitions live in the same B-tree as property index definitions, whose keys
//! are `[kind, scope_id, property_id]`. The leading byte already separates node
//! definitions from edge ones, so full-text definitions need only their own
//! values of it. No new tree, no new slot in the file header, and no change to
//! how anything already stored is read.

const std = @import("std");
const lattice = @import("lattice");

const BTree = lattice.storage.btree.BTree;
const BTreeError = lattice.storage.btree.BTreeError;
const SymbolId = lattice.graph.symbols.SymbolId;

/// Kind discriminators for full-text definitions.
///
/// Continues the numbering used by property index definitions, which take 1 for
/// nodes and 2 for edges, so the two kinds of index share one key space without
/// colliding.
pub const FtsEntityKind = enum(u8) {
    node = 3,
    edge = 4,
};

pub const FtsCatalogError = error{
    NotFound,
    AlreadyExists,
    InvalidData,
    IoError,
    OutOfMemory,
};

const DEFINITION_KEY_SIZE = 5;

/// One declared full-text index.
pub const FtsDefinition = struct {
    kind: FtsEntityKind,
    /// Label for a node index, edge type for an edge one.
    scope_id: SymbolId,
    /// The property holding the text.
    property_id: SymbolId,
};

/// The declared full-text indexes, stored in the shared index catalog.
pub const FtsCatalog = struct {
    catalog: *BTree,

    const Self = @This();

    pub fn init(catalog: *BTree) Self {
        return .{ .catalog = catalog };
    }

    pub fn create(self: *Self, definition: FtsDefinition) FtsCatalogError!void {
        const key = definitionKey(definition);
        if (self.catalog.contains(&key) catch return FtsCatalogError.IoError) {
            return FtsCatalogError.AlreadyExists;
        }
        self.catalog.insert(&key, &.{}) catch |err| return mapBTreeError(err);
    }

    pub fn drop(self: *Self, definition: FtsDefinition) FtsCatalogError!void {
        const key = definitionKey(definition);
        self.catalog.delete(&key) catch |err| return mapBTreeError(err);
    }

    pub fn has(self: *Self, definition: FtsDefinition) FtsCatalogError!bool {
        const key = definitionKey(definition);
        return self.catalog.contains(&key) catch return FtsCatalogError.IoError;
    }

    /// A walk over the declarations of one kind.
    ///
    /// The iterator owns its bounds. The tree's iterator keeps the end key as a
    /// slice and compares against it on every step, so bounds built in a local and
    /// handed out by slice leave it reading freed stack — which does not crash, it
    /// just quietly ends the walk early or never starts it, and the caller sees a
    /// catalog with nothing in it.
    pub const DefinitionIterator = struct {
        inner: BTree.Iterator,
        start: [1]u8 = undefined,
        end: [1]u8 = undefined,

        pub fn next(self: *DefinitionIterator) FtsCatalogError!?FtsDefinition {
            const entry = self.inner.next() catch |err| return mapBTreeError(err);
            const found = entry orelse return null;
            if (found.key.len != DEFINITION_KEY_SIZE) return FtsCatalogError.InvalidData;

            const kind: FtsEntityKind = switch (found.key[0]) {
                @intFromEnum(FtsEntityKind.node) => .node,
                @intFromEnum(FtsEntityKind.edge) => .edge,
                else => return FtsCatalogError.InvalidData,
            };

            return FtsDefinition{
                .kind = kind,
                .scope_id = std.mem.readInt(u16, found.key[1..3], .big),
                .property_id = std.mem.readInt(u16, found.key[3..5], .big),
            };
        }

        pub fn deinit(self: *DefinitionIterator) void {
            self.inner.deinit();
        }
    };

    /// Walk every declared index of one kind.
    ///
    /// The iterator is written into `out` rather than returned, because it holds
    /// the bounds the walk compares against and moving it would leave those
    /// comparisons pointing at the old copy.
    pub fn iterate(self: *Self, kind: FtsEntityKind, out: *DefinitionIterator) FtsCatalogError!void {
        out.start = .{@intFromEnum(kind)};
        out.end = .{@intFromEnum(kind) + 1};
        out.inner = self.catalog.range(&out.start, &out.end) catch |err| return mapBTreeError(err);
    }

    /// Find the index declared for a property, if there is one.
    ///
    /// This is what `@@` asks. A null answer means no index was declared, which
    /// callers should report rather than treat as "nothing matched": those two
    /// look identical from the outside and confusing them is the whole reason
    /// this exists.
    pub fn find(
        self: *Self,
        kind: FtsEntityKind,
        scope_id: SymbolId,
        property_id: SymbolId,
    ) FtsCatalogError!?FtsDefinition {
        const definition = FtsDefinition{
            .kind = kind,
            .scope_id = scope_id,
            .property_id = property_id,
        };
        if (try self.has(definition)) return definition;
        return null;
    }
};

fn definitionKey(definition: FtsDefinition) [DEFINITION_KEY_SIZE]u8 {
    var key: [DEFINITION_KEY_SIZE]u8 = undefined;
    key[0] = @intFromEnum(definition.kind);
    std.mem.writeInt(u16, key[1..3], definition.scope_id, .big);
    std.mem.writeInt(u16, key[3..5], definition.property_id, .big);
    return key;
}

fn mapBTreeError(err: BTreeError) FtsCatalogError {
    return switch (err) {
        BTreeError.KeyNotFound => FtsCatalogError.NotFound,
        BTreeError.DuplicateKey => FtsCatalogError.AlreadyExists,
        BTreeError.OutOfMemory => FtsCatalogError.OutOfMemory,
        else => FtsCatalogError.IoError,
    };
}

/// The prefix that scopes stored terms and documents to one declared index.
///
/// The dictionary, document lengths, and reverse index are shared trees, so every
/// key written for an index carries this in front of it. Without it a term
/// indexed for one property would be found when searching another, which is the
/// behaviour being replaced.
pub const SCOPE_PREFIX_SIZE = 4;

pub fn scopePrefix(definition: FtsDefinition) [SCOPE_PREFIX_SIZE]u8 {
    var prefix: [SCOPE_PREFIX_SIZE]u8 = undefined;
    std.mem.writeInt(u16, prefix[0..2], definition.scope_id, .big);
    std.mem.writeInt(u16, prefix[2..4], definition.property_id, .big);
    return prefix;
}

test "definitions of different kinds do not collide" {
    const node = FtsDefinition{ .kind = .node, .scope_id = 7, .property_id = 9 };
    const edge = FtsDefinition{ .kind = .edge, .scope_id = 7, .property_id = 9 };

    const node_key = definitionKey(node);
    const edge_key = definitionKey(edge);
    try std.testing.expect(!std.mem.eql(u8, &node_key, &edge_key));

    // And neither may look like a property index definition, which uses 1 and 2
    // in the same byte of the same tree.
    try std.testing.expect(node_key[0] != 1 and node_key[0] != 2);
    try std.testing.expect(edge_key[0] != 1 and edge_key[0] != 2);
}

test "keys order by kind, so an iterator sees one kind at a time" {
    const a = definitionKey(.{ .kind = .node, .scope_id = 1, .property_id = 1 });
    const b = definitionKey(.{ .kind = .node, .scope_id = 65535, .property_id = 65535 });
    const c = definitionKey(.{ .kind = .edge, .scope_id = 0, .property_id = 0 });

    try std.testing.expect(std.mem.order(u8, &a, &b) == .lt);
    try std.testing.expect(std.mem.order(u8, &b, &c) == .lt);
}

test "scope prefixes separate one property from another" {
    const title = scopePrefix(.{ .kind = .node, .scope_id = 3, .property_id = 1 });
    const body = scopePrefix(.{ .kind = .node, .scope_id = 3, .property_id = 2 });
    const other_label = scopePrefix(.{ .kind = .node, .scope_id = 4, .property_id = 1 });

    try std.testing.expect(!std.mem.eql(u8, &title, &body));
    try std.testing.expect(!std.mem.eql(u8, &title, &other_label));
}
