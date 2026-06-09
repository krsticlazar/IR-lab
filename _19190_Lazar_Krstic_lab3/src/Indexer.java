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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;

public class Indexer {
	public static void main(String[] args) {
		try {
			Indexer indexer = new Indexer();
			printStats(indexer.createTextIndex());
			printStats(indexer.createTikaIndex());
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public IndexStats createTextIndex() throws IOException {
		Path dataDirectory = Path.of(BaseConfig.DIREKTORIJUM_TXT_PODACI);
		Path indexDirectory = Path.of(BaseConfig.DIREKTORIJUM_TXT_INDEKS);
		List<Path> files = BaseConfig.listTextFiles(dataDirectory);
		return createIndex(dataDirectory, indexDirectory, files, false);
	}

	public IndexStats createTikaIndex() throws IOException {
		Path dataDirectory = Path.of(BaseConfig.DIREKTORIJUM_TIKA_PODACI);
		Path indexDirectory = Path.of(BaseConfig.DIREKTORIJUM_TIKA_INDEKS);
		List<Path> files = BaseConfig.listTikaFiles(dataDirectory);
		return createIndex(dataDirectory, indexDirectory, files, true);
	}

	private IndexStats createIndex(Path dataDirectory, Path indexDirectory, List<Path> files, boolean useTika)
			throws IOException {
		if (files.isEmpty()) {
			throw new IOException("Nema fajlova za indeksiranje u folderu " + dataDirectory.toAbsolutePath());
		}

		Files.createDirectories(indexDirectory);
		long start = System.nanoTime();
		try (Analyzer analyzer = BaseConfig.createAnalyzer();
				Directory directory = FSDirectory.open(indexDirectory)) {
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			config.setOpenMode(OpenMode.CREATE);                                         // svaki run pravi nov indeks bez rucnog brisanja

			try (IndexWriter writer = new IndexWriter(directory, config)) {
				Tika tika = useTika ? new Tika() : null;
				for (Path file : files) {
					if (useTika) {
						indexTikaFile(writer, file, tika);
					} else {
						indexTextFile(writer, file);
					}
				}
			}
		}

		long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
		long indexSize = BaseConfig.directorySize(indexDirectory);
		return new IndexStats(dataDirectory, indexDirectory, files.size(), elapsedMillis, indexSize);
	}

	private void indexTextFile(IndexWriter writer, Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			writer.addDocument(createDocument(file, reader, "txt"));
		}
	}

	private void indexTikaFile(IndexWriter writer, Path file, Tika tika) throws IOException {
		try (Reader reader = tika.parse(file)) {                                        // Tika izvlaci tekst iz PDF/HTML/RTF i slicnih fajlova
			writer.addDocument(createDocument(file, reader, BaseConfig.extensionOf(file).replace(".", "")));
		}
	}

	private Document createDocument(Path file, Reader contentReader, String format) throws IOException {
		String fileName = file.getFileName().toString();
		String title = BaseConfig.titleFromFile(file);
		String normalizedTitle = BaseConfig.normalizeFileNameForRange(fileName);
		String fullPath = file.toAbsolutePath().normalize().toString();
		long fileSize = Files.size(file);

		Document document = new Document();
		document.add(new TextField(BaseConfig.POLJE_SADRZAJ, contentReader));            // analizirano polje nad kojim se izvrsava pretraga
		document.add(new StringField(BaseConfig.POLJE_NAZIV, title, Field.Store.YES));
		document.add(new StringField(BaseConfig.POLJE_NAZIV_SORT, normalizedTitle, Field.Store.YES));
		document.add(new StringField(BaseConfig.POLJE_PUTANJA, fullPath, Field.Store.YES));
		document.add(new StringField(BaseConfig.POLJE_VELICINA, Long.toString(fileSize), Field.Store.YES));
		document.add(new StringField(BaseConfig.POLJE_FORMAT, format, Field.Store.YES));
		return document;
	}

	public static void printStats(IndexStats stats) {
		System.out.println();
		System.out.println("Indeks: " + stats.indexDirectory());
		System.out.println("Podaci: " + stats.dataDirectory());
		System.out.println("Broj dokumenata: " + stats.documentCount());
		System.out.println("Vreme kreiranja: " + stats.elapsedMillis() + " ms");
		System.out.println("Velicina indeksa: " + BaseConfig.formatBytes(stats.indexSizeBytes()));
	}

	public record IndexStats(Path dataDirectory, Path indexDirectory, int documentCount, long elapsedMillis,
			long indexSizeBytes) {
	}
}
