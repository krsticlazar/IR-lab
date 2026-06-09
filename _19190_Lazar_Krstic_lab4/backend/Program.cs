using Lab4.Api.Models;
using Lab4.Api.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddSingleton<DocumentCatalog>();
builder.Services.AddHttpClient<ElasticsearchService>((services, client) =>
{
    var configuration = services.GetRequiredService<IConfiguration>();
    var baseUrl = configuration["Elasticsearch:BaseUrl"] ?? "http://localhost:9200";
    client.BaseAddress = new Uri(baseUrl);
});
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        var origin = builder.Configuration["Cors:FrontendOrigin"] ?? "http://localhost:5173";
        policy.WithOrigins(origin).AllowAnyHeader().AllowAnyMethod();
    });
});

var app = builder.Build();

app.UseCors();

app.MapGet("/api/health", async (ElasticsearchService elasticsearch, CancellationToken cancellationToken) =>
{
    var available = await elasticsearch.IsAvailableAsync(cancellationToken);
    return Results.Ok(new { elasticsearch = available ? "available" : "unavailable" });
});

app.MapGet("/api/fields", (DocumentCatalog catalog) => Results.Ok(catalog.SearchFields));

app.MapGet("/api/index/status", async (DocumentCatalog catalog, ElasticsearchService elasticsearch,
    CancellationToken cancellationToken) =>
{
    var status = await elasticsearch.GetStatusAsync(catalog.DocumentsDirectory, cancellationToken);
    return Results.Ok(status);
});

app.MapPost("/api/index/rebuild", async (DocumentCatalog catalog, ElasticsearchService elasticsearch,
    CancellationToken cancellationToken) =>
{
    var indexedAt = DateTime.Now;
    var documents = catalog.LoadDocuments(indexedAt);
    var summary = await elasticsearch.RebuildIndexAsync(documents, catalog.DocumentsDirectory, indexedAt,
        cancellationToken);
    return Results.Ok(summary);
});

app.MapPost("/api/search", async (SearchRequest request, DocumentCatalog catalog, ElasticsearchService elasticsearch,
    CancellationToken cancellationToken) =>
{
    var allowedFields = catalog.SearchFields.Select(field => field.Value).ToHashSet(StringComparer.OrdinalIgnoreCase);
    var response = await elasticsearch.SearchAsync(request, allowedFields, cancellationToken);
    return Results.Ok(response);
});

app.MapGet("/api/documents/{id}/download", (string id, DocumentCatalog catalog) =>
{
    var path = catalog.FindDocumentPath(id);
    if (path is null)
    {
        return Results.NotFound();
    }

    return Results.File(path, "text/plain; charset=utf-8", Path.GetFileName(path));      // link iz rezultata otvara/preuzima dokument
});

app.Run();
