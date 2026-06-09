import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {
	public static void main(String[] args) {
		try {
			Indexer indexer = new Indexer();
			IndexStats bm25Stats = indexer.createIndex(
					Path.of(BaseConfig.DIREKTORIJUM_PODACI),
					Path.of(BaseConfig.DIREKTORIJUM_INDEKS_BM25),
					new BM25Similarity(),
					"BM25Similarity");
			IndexStats classicStats = indexer.createIndex(
					Path.of(BaseConfig.DIREKTORIJUM_PODACI),
					Path.of(BaseConfig.DIREKTORIJUM_INDEKS_CLASSIC),
					new ClassicSimilarity(),
					"ClassicSimilarity");

			printStats(bm25Stats);
			printStats(classicStats);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public IndexStats createIndex(Path dataDirectory, Path indexDirectory, Similarity similarity,
			String similarityName) throws IOException {
		Files.createDirectories(indexDirectory);
		List<Path> files = BaseConfig.listTextFiles(dataDirectory);
		if (files.isEmpty()) {
			throw new IOException("Nema .txt fajlova za indeksiranje u folderu " + dataDirectory.toAbsolutePath());
		}

		long start = System.nanoTime();
		try (Analyzer analyzer = BaseConfig.createAnalyzer();
				Directory directory = FSDirectory.open(indexDirectory)) {
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			config.setOpenMode(OpenMode.CREATE);
			config.setSimilarity(similarity);                                            // mera slicnosti se mora zadati vec pri indeksiranju

			try (IndexWriter writer = new IndexWriter(directory, config)) {
				for (Path file : files) {
					indexFile(writer, file);
				}
			}
		}

		long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
		long indexSize = BaseConfig.directorySize(indexDirectory);
		return new IndexStats(dataDirectory, indexDirectory, similarityName, files.size(), elapsedMillis, indexSize);
	}

	private void indexFile(IndexWriter writer, Path file) throws IOException {
		String title = BaseConfig.titleFromFile(file);
		String fullPath = file.toAbsolutePath().normalize().toString();
		long fileSize = Files.size(file);

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			Document document = new Document();
			document.add(new StringField(BaseConfig.POLJE_PUTANJA, fullPath, Field.Store.YES));
			document.add(new TextField(BaseConfig.POLJE_NASLOV, title, Field.Store.YES)); // TextField jer se naslov pretrazuje terminima
			document.add(new StringField(BaseConfig.POLJE_VELICINA, Long.toString(fileSize), Field.Store.YES));
			document.add(new TextField(BaseConfig.POLJE_SADRZAJ, reader));                // sadrzaj se indeksira, ali se ne cuva u rezultatu
			writer.addDocument(document);
		}
	}

	public static void printStats(IndexStats stats) {
		System.out.println();
		System.out.println("Indeks: " + stats.indexDirectory());
		System.out.println("Similarity: " + stats.similarityName());
		System.out.println("Podaci: " + stats.dataDirectory());
		System.out.println("Broj dokumenata: " + stats.documentCount());
		System.out.println("Vreme kreiranja: " + stats.elapsedMillis() + " ms");
		System.out.println("Velicina indeksa: " + BaseConfig.formatBytes(stats.indexSizeBytes()));
	}

	public record IndexStats(Path dataDirectory, Path indexDirectory, String similarityName, int documentCount,
			long elapsedMillis, long indexSizeBytes) {
	}
}
