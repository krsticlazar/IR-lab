import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public final class BaseConfig {
	public static final String DIREKTORIJUM_PODACI = "PodaciOriginalni";
	public static final String DIREKTORIJUM_INDEKS_BM25 = "IndeksBM25";
	public static final String DIREKTORIJUM_INDEKS_CLASSIC = "IndeksClassic";

	public static final String POLJE_PUTANJA = "putanja";
	public static final String POLJE_NASLOV = "naslov";
	public static final String POLJE_SADRZAJ = "sadrzaj";
	public static final String POLJE_VELICINA = "velicina";

	public static final int BROJ_REZULTATA_ZA_PRIKAZ = 10;
	public static final String[] TERMINI_UPITA = { "yellow", "wallpaper" };             // isti bulovski upit radi nad naslovom i sadrzajem
	public static final String OCEKIVANI_ZAJEDNICKI_DOKUMENT = "The Yellow Wallpaper - Charlotte Perkins Gilman";

	private BaseConfig() {
	}

	public static Analyzer createAnalyzer() {                                           // isti analyzer mora da se koristi pri indeksiranju i pretrazi
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

	public static String titleFromFile(Path file) {
		return removeTxtExtension(file.getFileName().toString());
	}

	public static String removeTxtExtension(String fileName) {
		if (fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
			return fileName.substring(0, fileName.length() - 4);
		}
		return fileName;
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

	public static String formatScore(float score) {
		return String.format(Locale.US, "%.6f", score);
	}

	private static long safeFileSize(Path path) {
		try {
			return Files.size(path);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
}
