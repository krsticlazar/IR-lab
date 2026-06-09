using System.Net;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using Lab4.Api.Models;

namespace Lab4.Api.Services;

public sealed class ElasticsearchService
{
    public const int PageSize = 5;

    private readonly HttpClient httpClient;
    private readonly string indexName;
    private readonly JsonSerializerOptions jsonOptions = new(JsonSerializerDefaults.Web);

    public ElasticsearchService(HttpClient httpClient, IConfiguration configuration)
    {
        this.httpClient = httpClient;
        indexName = configuration["Elasticsearch:IndexName"] ?? "ir_lab4_19190";
    }

    public string IndexName => indexName;

    public async Task<bool> IsAvailableAsync(CancellationToken cancellationToken)
    {
        try
        {
            using var response = await httpClient.GetAsync("/", cancellationToken);
            return response.IsSuccessStatusCode;
        }
        catch
        {
            return false;
        }
    }

    public async Task<IndexSummary> RebuildIndexAsync(IReadOnlyList<DocumentRecord> documents, string documentsDirectory,
        DateTime indexedAt, CancellationToken cancellationToken)
    {
        await DeleteIndexIfExistsAsync(cancellationToken);
        await CreateIndexAsync(cancellationToken);
        await BulkIndexAsync(documents, cancellationToken);                              // _bulk je obavezan za efikasno slanje 400 dokumenata

        return new IndexSummary(documents.Count, indexName, documentsDirectory, indexedAt);
    }

    public async Task<IndexStatus> GetStatusAsync(string documentsDirectory, CancellationToken cancellationToken)
    {
        using var existsResponse = await httpClient.SendAsync(new HttpRequestMessage(HttpMethod.Head, indexName),
            cancellationToken);
        if (existsResponse.StatusCode == HttpStatusCode.NotFound)
        {
            return new IndexStatus(false, 0, indexName, documentsDirectory);
        }

        existsResponse.EnsureSuccessStatusCode();
        using var countResponse = await httpClient.GetAsync($"{indexName}/_count", cancellationToken);
        countResponse.EnsureSuccessStatusCode();

        using var json = await JsonDocument.ParseAsync(await countResponse.Content.ReadAsStreamAsync(cancellationToken),
            cancellationToken: cancellationToken);
        var count = json.RootElement.GetProperty("count").GetInt64();
        return new IndexStatus(true, count, indexName, documentsDirectory);
    }

    public async Task<SearchResponse> SearchAsync(SearchRequest request, IReadOnlySet<string> allowedFields,
        CancellationToken cancellationToken)
    {
        var field = string.IsNullOrWhiteSpace(request.Field) ? "content" : request.Field;
        if (!allowedFields.Contains(field))
        {
            throw new InvalidOperationException($"Polje nije dozvoljeno za pretragu: {field}");
        }

        var page = Math.Max(1, request.Page);
        var queryText = request.Query?.Trim() ?? "";
        object query = string.IsNullOrWhiteSpace(queryText)
            ? new { match_all = new { } }
            : new { match = new Dictionary<string, object> { [field] = new { query = queryText } } };
        var searchBody = new
        {
            from = (page - 1) * PageSize,
            size = PageSize,
            track_total_hits = true,
            query,
            sort = new object[] { "_score", new Dictionary<string, object> { ["fileName.keyword"] = "asc" } }
        };

        using var response = await httpClient.PostAsJsonAsync($"{indexName}/_search", searchBody, jsonOptions,
            cancellationToken);
        response.EnsureSuccessStatusCode();

        using var json = await JsonDocument.ParseAsync(await response.Content.ReadAsStreamAsync(cancellationToken),
            cancellationToken: cancellationToken);
        var hits = json.RootElement.GetProperty("hits");
        var total = hits.GetProperty("total").GetProperty("value").GetInt64();
        var results = hits.GetProperty("hits").EnumerateArray()
            .Select(ReadSearchHit)
            .ToList();
        var totalPages = total == 0 ? 0 : (int)Math.Ceiling(total / (double)PageSize);

        return new SearchResponse(results, total, page, PageSize, totalPages, field, queryText);
    }

    private async Task DeleteIndexIfExistsAsync(CancellationToken cancellationToken)
    {
        using var response = await httpClient.DeleteAsync(indexName, cancellationToken);
        if (response.StatusCode != HttpStatusCode.NotFound)
        {
            response.EnsureSuccessStatusCode();
        }
    }

    private async Task CreateIndexAsync(CancellationToken cancellationToken)
    {
        var body = new
        {
            mappings = new
            {
                properties = new Dictionary<string, object>
                {
                    ["id"] = new { type = "keyword" },
                    ["fileName"] = new { type = "text", fields = new { keyword = new { type = "keyword" } } },
                    ["title"] = new { type = "text" },
                    ["path"] = new { type = "text", fields = new { keyword = new { type = "keyword" } } },
                    ["sizeBytes"] = new { type = "long" },
                    ["sizeText"] = new { type = "text" },
                    ["modifiedAt"] = new { type = "date" },
                    ["modifiedAtText"] = new { type = "text" },
                    ["indexedAt"] = new { type = "date" },
                    ["indexedAtText"] = new { type = "text" },
                    ["content"] = new { type = "text" },
                    ["preview"] = new { type = "text", index = false }
                }
            }
        };

        using var response = await httpClient.PutAsJsonAsync(indexName, body, jsonOptions, cancellationToken);
        response.EnsureSuccessStatusCode();
    }

    private async Task BulkIndexAsync(IReadOnlyList<DocumentRecord> documents, CancellationToken cancellationToken)
    {
        var builder = new StringBuilder();
        foreach (var document in documents)
        {
            builder.Append(JsonSerializer.Serialize(new
            {
                index = new Dictionary<string, object>
                {
                    ["_index"] = indexName,
                    ["_id"] = document.Id
                }
            }, jsonOptions));
            builder.Append('\n');
            builder.Append(JsonSerializer.Serialize(document, jsonOptions));
            builder.Append('\n');
        }

        using var content = new StringContent(builder.ToString(), Encoding.UTF8, "application/x-ndjson");
        using var response = await httpClient.PostAsync("_bulk?refresh=true", content, cancellationToken);
        response.EnsureSuccessStatusCode();

        using var json = await JsonDocument.ParseAsync(await response.Content.ReadAsStreamAsync(cancellationToken),
            cancellationToken: cancellationToken);
        if (json.RootElement.GetProperty("errors").GetBoolean())
        {
            throw new InvalidOperationException("Elasticsearch _bulk indeksiranje je vratilo gresku.");
        }
    }

    private static SearchResultItem ReadSearchHit(JsonElement hit)
    {
        var source = hit.GetProperty("_source");
        var id = source.GetProperty("id").GetString() ?? "";
        return new SearchResultItem(
            Id: id,
            FileName: source.GetProperty("fileName").GetString() ?? "",
            Title: source.GetProperty("title").GetString() ?? "",
            SizeBytes: source.GetProperty("sizeBytes").GetInt64(),
            SizeText: source.GetProperty("sizeText").GetString() ?? "",
            ModifiedAtText: source.GetProperty("modifiedAtText").GetString() ?? "",
            IndexedAtText: source.GetProperty("indexedAtText").GetString() ?? "",
            Preview: source.GetProperty("preview").GetString() ?? "",
            Score: hit.TryGetProperty("_score", out var score) && score.ValueKind == JsonValueKind.Number
                ? score.GetDouble()
                : null,
            DownloadUrl: $"/api/documents/{id}/download");
    }
}
