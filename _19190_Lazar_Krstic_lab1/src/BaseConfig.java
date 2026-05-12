import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public final class BaseConfig {
	public static final String DIREKTORIJUM_ORIGINALNI_PODACI = "PodaciOriginalni";
	public static final String DIREKTORIJUM_DELOVI_PODACI = "PodaciDelovi";
	public static final String DIREKTORIJUM_ORIGINALNI_INDEKS = "IndeksOriginalni";
	public static final String DIREKTORIJUM_DELOVI_INDEKS = "IndeksDelovi";

	public static final String POLJE_SADRZAJ = "sadrzaj";                               // tekst fajla koji se tokenizuje i pretrazuje
	public static final String POLJE_NAZIV = "naziv";
	public static final String POLJE_PUTANJA = "putanja";
	public static final String POLJE_VELICINA = "velicina";
	public static final String POLJE_NAZIV_SORT = "naziv_sort";                         // normalizovan naziv za TermRangeQuery

	public static final int BROJ_DELOVA_PO_FAJLU = 100;                                  // svaki originalni fajl deli se na 100 delova
	public static final int BROJ_REZULTATA_ZA_PRIKAZ = 10;

	private BaseConfig() {
	}

	public static Analyzer createAnalyzer() {                                           // isti analyzer se koristi za indeksiranje i upite
		return new StandardAnalyzer();
	}

	public static List<Path> listTextFiles(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			throw new IOException("Direktorijum ne postoji: " + directory.toAbsolutePath());
		}

		try (Stream<Path> stream = Files.list(directory)) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
					.sorted()
					.toList();
		}
	}

	public static long directorySize(Path directory) throws IOException {                // racuna ukupnu velicinu indeksa na disku
		if (!Files.exists(directory)) {
			return 0L;
		}

		try (Stream<Path> stream = Files.walk(directory)) {
			return stream.filter(Files::isRegularFile)
					.mapToLong(BaseConfig::safeFileSize)
					.sum();
		} catch (UncheckedIOException exception) {
			throw exception.getCause();
		}
	}

	public static String normalizeFileNameForRange(String fileName) {                   // priprema naziv za leksikografski range upit
		String withoutExtension = removeTxtExtension(fileName);
		String ascii = Normalizer.normalize(withoutExtension, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		String normalized = ascii.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+", "")
				.replaceAll("_+$", "");

		return normalized.isBlank() ? "fajl" : normalized;
	}

	public static String removeTxtExtension(String fileName) {
		if (fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
			return fileName.substring(0, fileName.length() - 4);
		}
		return fileName;
	}

	public static String formatBytes(long bytes) {
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
		DecimalFormat format = new DecimalFormat("#,##0", symbols);
		return format.format(bytes) + " B";
	}

	private static long safeFileSize(Path path) {
		try {
			return Files.size(path);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
}
