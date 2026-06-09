namespace Lab4.Api.Models;

public sealed record SearchRequest(string Field, string Query, int Page = 1);

public sealed record SearchResultItem(
    string Id,
    string FileName,
    string Title,
    long SizeBytes,
    string SizeText,
    string ModifiedAtText,
    string IndexedAtText,
    string Preview,
    double? Score,
    string DownloadUrl);

public sealed record SearchResponse(
    IReadOnlyList<SearchResultItem> Results,
    long Total,
    int Page,
    int PageSize,
    int TotalPages,
    string Field,
    string Query);
