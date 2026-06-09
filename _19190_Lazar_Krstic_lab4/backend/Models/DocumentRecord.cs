namespace Lab4.Api.Models;

public sealed record DocumentRecord(
    string Id,
    string FileName,
    string Title,
    string Path,
    long SizeBytes,
    string SizeText,
    DateTime ModifiedAt,
    string ModifiedAtText,
    DateTime IndexedAt,
    string IndexedAtText,
    string Content,
    string Preview);
