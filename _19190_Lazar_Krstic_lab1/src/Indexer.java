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

public class Indexer {                                                                 // kreira Lucene indekse nad txt kolekcijama
	public static void main(String[] args) {
		try {
			Indexer indexer = new Indexer();
			if (args.length == 2) {
				IndexStats stats = indexer.createIndex(Path.of(args[0]), Path.of(args[1]));
				printStats(stats);
				return;
			}

			if (args.length != 0) {
				throw new IllegalArgumentException("Upotreba: Indexer [folder_sa_podacima folder_indeksa]");
			}

			IndexStats originalStats = indexer.createIndex(                              // kreira indeks za 4 originalna fajla
					Path.of(BaseConfig.DIREKTORIJUM_ORIGINALNI_PODACI),
					Path.of(BaseConfig.DIREKTORIJUM_ORIGINALNI_INDEKS));
			IndexStats splitStats = indexer.createIndex(                                 // kreira indeks za 400 delova
					Path.of(BaseConfig.DIREKTORIJUM_DELOVI_PODACI),
					Path.of(BaseConfig.DIREKTORIJUM_DELOVI_INDEKS));

			printStats(originalStats);
			printStats(splitStats);
			printComparison(originalStats, splitStats);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public IndexStats createIndex(Path dataDirectory, Path indexDirectory) throws IOException { // centralna metoda za jedan indeks
		Files.createDirectories(indexDirectory);
		List<Path> files = BaseConfig.listTextFiles(dataDirectory);
		if (files.isEmpty()) {                                                           // indeks bez dokumenata nema smisla
			throw new IOException("Nema .txt fajlova za indeksiranje u folderu " + dataDirectory.toAbsolutePath());
		}

		long start = System.nanoTime();
		try (Analyzer analyzer = BaseConfig.createAnalyzer();
				Directory directory = FSDirectory.open(indexDirectory)) {
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			config.setOpenMode(OpenMode.CREATE);                                         // svaki put pravi nov indeks, bez rucnog brisanja

			try (IndexWriter writer = new IndexWriter(directory, config)) {
				for (Path file : files) {                                                // svaki txt fajl postaje jedan Lucene dokument
					indexFile(writer, file);
				}
			}
		}
		long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
		long indexSize = BaseConfig.directorySize(indexDirectory);

		return new IndexStats(dataDirectory, indexDirectory, files.size(), elapsedMillis, indexSize);
	}

	private void indexFile(IndexWriter writer, Path file) throws IOException {           // pretvara jedan txt fajl u Lucene Document
		long fileSize = Files.size(file);                                                // obavezno polje: velicina u bajtovima
		String fileName = file.getFileName().toString();                                 // obavezno polje: naziv fajla
		String fullPath = file.toAbsolutePath().normalize().toString();                  // obavezno polje: puna putanja
		String normalizedFileName = BaseConfig.normalizeFileNameForRange(fileName);      // dodatno polje za TermRangeQuery

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {     // cita sadrzaj kao UTF-8 tekst
			Document document = new Document();                                           // Lucene dokument odgovara jednom fajlu
			document.add(new TextField(BaseConfig.POLJE_SADRZAJ, reader));               // TextField se analizira i ide u invertovani indeks
			document.add(new StringField(BaseConfig.POLJE_NAZIV, fileName, Field.Store.YES)); // StringField se cuva kao jedan token
			document.add(new StringField(BaseConfig.POLJE_PUTANJA, fullPath, Field.Store.YES)); // putanja se cuva za prikaz rezultata
			document.add(new StringField(BaseConfig.POLJE_VELICINA, Long.toString(fileSize), Field.Store.YES)); // velicina se cuva za prikaz
			document.add(new StringField(BaseConfig.POLJE_NAZIV_SORT, normalizedFileName, Field.Store.YES)); // tekstualno polje za opseg
			writer.addDocument(document);                                                // dokument se fizicki dodaje u indeks
		}
	}

	public static void printStats(IndexStats stats) {
		System.out.println();
		System.out.println("Indeks: " + stats.indexDirectory());
		System.out.println("Podaci: " + stats.dataDirectory());
		System.out.println("Broj dokumenata: " + stats.documentCount());
		System.out.println("Vreme kreiranja: " + stats.elapsedMillis() + " ms");
		System.out.println("Velicina indeksa: " + BaseConfig.formatBytes(stats.indexSizeBytes()));
	}

	public static void printComparison(IndexStats originalStats, IndexStats splitStats) {
		System.out.println();
		System.out.println("Poredjenje indeksa");
		System.out.println("Dokumenti       | original: " + originalStats.documentCount()
				+ " | delovi: " + splitStats.documentCount());
		System.out.println("Velicina indeksa | original: " + BaseConfig.formatBytes(originalStats.indexSizeBytes())
				+ " | delovi: " + BaseConfig.formatBytes(splitStats.indexSizeBytes())
				+ " | odnos: " + String.format("%.2f%%", splitStats.indexSizeBytes() * 100.0 / originalStats.indexSizeBytes()-100));
		System.out.println("Vreme kreiranja | original: " + originalStats.elapsedMillis() + " ms"
				+ " | delovi: " + splitStats.elapsedMillis() + " ms"
				+ " | odnos: " + String.format("%.2f%%", splitStats.elapsedMillis() * 100.0 / originalStats.elapsedMillis()-100));
	}

	public record IndexStats(Path dataDirectory, Path indexDirectory, int documentCount, long elapsedMillis,
			long indexSizeBytes) {
	}
}
