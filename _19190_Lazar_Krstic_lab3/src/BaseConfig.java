import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public final class BaseConfig {
	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
		System.setProperty("org.slf4j.simpleLogger.log.org.apache.pdfbox", "warn");
	}

	public static final String DIREKTORIJUM_TXT_PODACI = "PodaciTxt";
	public static final String DIREKTORIJUM_TIKA_PODACI = "PodaciTika";
	public static final String DIREKTORIJUM_TXT_INDEKS = "IndeksTxt";
	public static final String DIREKTORIJUM_TIKA_INDEKS = "IndeksTika";

	public static final String POLJE_SADRZAJ = "sadrzaj";
	public static final String POLJE_NAZIV = "naziv";
	public static final String POLJE_NAZIV_SORT = "naziv_sort";                         // StringField namenjen TermRangeQuery upitu
	public static final String POLJE_PUTANJA = "putanja";
	public static final String POLJE_VELICINA = "velicina";
	public static final String POLJE_FORMAT = "format";

	public static final int BROJ_REZULTATA_ZA_PRIKAZ = 10;
	public static final Set<String> TIKA_EKSTENZIJE = Set.of(".html", ".htm", ".rtf", ".pdf", ".doc", ".docx",
			".epub", ".ppt", ".pptx");

	private BaseConfig() {
	}

	public static Analyzer createAnalyzer() {                                           // isti analyzer se koristi za indeksiranje i parser
		return new StandardAnalyzer();
	}

	public static List<Path> listTextFiles(Path directory) throws IOException {
		return listFilesByExtensions(directory, Set.of(".txt"));
	}

	public static List<Path> listTikaFiles(Path directory) throws IOException {
		return listFilesByExtensions(directory, TIKA_EKSTENZIJE);
	}

	private static List<Path> listFilesByExtensions(Path directory, Set<String> extensions) throws IOException {
		if (!Files.isDirectory(directory)) {
			throw new IOException("Direktorijum ne postoji: " + directory.toAbsolutePath());
		}

		try (Stream<Path> stream = Files.list(directory)) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> extensions.contains(extensionOf(path)))
					.sorted()
					.toList();
		}
	}

	public static String titleFromFile(Path file) {
		String fileName = file.getFileName().toString();
		int dotIndex = fileName.lastIndexOf('.');
		return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
	}

	public static String extensionOf(Path file) {
		String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
		int dotIndex = fileName.lastIndexOf('.');
		return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
	}

	public static String normalizeFileNameForRange(String fileName) {                   // leksikografski stabilna vrednost za range upit
		String withoutExtension = titleFromFile(Path.of(fileName));
		String ascii = Normalizer.normalize(withoutExtension, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		String normalized = ascii.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+", "")
				.replaceAll("_+$", "");
		return normalized.isBlank() ? "dokument" : normalized;
	}

	public static long directorySize(Path directory) throws IOException {
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
