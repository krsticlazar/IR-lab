using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using Lab4.Api.Models;

namespace Lab4.Api.Services;

public sealed class DocumentCatalog
{
    private static readonly CultureInfo SerbianCulture = new("sr-Latn-RS");
    private readonly string documentsDirectory;

    public DocumentCatalog(IConfiguration configuration, IHostEnvironment environment)
    {
        var configuredPath = configuration["Documents:Directory"] ?? "../data/documents";
        documentsDirectory = Path.GetFullPath(Path.Combine(environment.ContentRootPath, configuredPath));
    }

    public string DocumentsDirectory => documentsDirectory;

    public IReadOnlyList<FieldOption> SearchFields { get; } =
    [
        new("content", "Sadrzaj"),
        new("fileName", "Naziv fajla"),
        new("title", "Naslov"),
        new("path", "Putanja"),
        new("sizeText", "Velicina"),
        new("modifiedAtText", "Datum poslednje izmene"),
        new("indexedAtText", "Datum indeksiranja")
    ];

    public IReadOnlyList<DocumentRecord> LoadDocuments(DateTime indexedAt)
    {
        if (!Directory.Exists(documentsDirectory))
        {
            throw new DirectoryNotFoundException($"Folder sa dokumentima ne postoji: {documentsDirectory}");
        }

        return Directory.EnumerateFiles(documentsDirectory, "*.txt", SearchOption.TopDirectoryOnly)
            .OrderBy(path => path, StringComparer.OrdinalIgnoreCase)
            .Select(path => CreateRecord(path, indexedAt))
            .ToList();
    }

    public string? FindDocumentPath(string id)
    {
        return Directory.EnumerateFiles(documentsDirectory, "*.txt", SearchOption.TopDirectoryOnly)
            .FirstOrDefault(path => CreateId(Path.GetFileName(path)).Equals(id, StringComparison.OrdinalIgnoreCase));
    }

    private static DocumentRecord CreateRecord(string path, DateTime indexedAt)
    {
        var fileName = Path.GetFileName(path);
        var title = Path.GetFileNameWithoutExtension(path);
        var content = File.ReadAllText(path, Encoding.UTF8);
        var fileInfo = new FileInfo(path);
        var modifiedAt = fileInfo.LastWriteTime;

        return new DocumentRecord(
            Id: CreateId(fileName),
            FileName: fileName,
            Title: title,
            Path: fileInfo.FullName,
            SizeBytes: fileInfo.Length,
            SizeText: FormatSize(fileInfo.Length),
            ModifiedAt: modifiedAt,
            ModifiedAtText: modifiedAt.ToString("dd.MM.yyyy.", SerbianCulture),
            IndexedAt: indexedAt,
            IndexedAtText: indexedAt.ToString("dd.MM.yyyy.", SerbianCulture),
            Content: content,
            Preview: BuildPreview(content));
    }

    private static string BuildPreview(string content)
    {
        var compact = string.Join(' ', content.Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries));
        return compact.Length <= 220 ? compact : compact[..220] + "...";
    }

    private static string FormatSize(long bytes)
    {
        var kilobytes = Math.Max(1, (int)Math.Ceiling(bytes / 1024.0));
        return $"{kilobytes} KB";
    }

    private static string CreateId(string fileName)
    {
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(fileName));
        return Convert.ToHexString(hash[..12]).ToLowerInvariant();
    }
}
