import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher implements AutoCloseable {
	private final Directory directory;
	private final IndexReader indexReader;
	private final IndexSearcher indexSearcher;

	public static void main(String[] args) {
		try {
			runAllSearches();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public static void runAllSearches() throws Exception {
		runSearchesForSimilarity("BM25Similarity", Path.of(BaseConfig.DIREKTORIJUM_INDEKS_BM25), new BM25Similarity());
		runSearchesForSimilarity("ClassicSimilarity", Path.of(BaseConfig.DIREKTORIJUM_INDEKS_CLASSIC),
				new ClassicSimilarity());
	}

	private static void runSearchesForSimilarity(String similarityName, Path indexDirectory, Similarity similarity)
			throws Exception {
		System.out.println();
		System.out.println("==================================================");
		System.out.println(similarityName + " - " + indexDirectory);
		System.out.println("==================================================");

		try (Searcher searcher = new Searcher(indexDirectory, similarity)) {
			Query titleQuery = createBooleanQuery(BaseConfig.POLJE_NASLOV);
			Query contentQuery = createBooleanQuery(BaseConfig.POLJE_SADRZAJ);

			List<SearchHit> titleHits = searcher.runQueryWithExplanation("Bulovski upit nad naslovom", titleQuery);
			List<SearchHit> contentHits = searcher.runQueryWithExplanation("Bulovski upit nad sadrzajem", contentQuery);
			searcher.compareScoresAndBoost(titleQuery, contentQuery, titleHits, contentHits);
		}
	}

	public Searcher(Path indexDirectory, Similarity similarity) throws IOException {
		this.directory = FSDirectory.open(indexDirectory);
		this.indexReader = DirectoryReader.open(directory);
		this.indexSearcher = new IndexSearcher(indexReader);
		this.indexSearcher.setSimilarity(similarity);                                   // ista mera slicnosti kao pri indeksiranju
	}

	private static Query createBooleanQuery(String fieldName) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for (String term : BaseConfig.TERMINI_UPITA) {
			builder.add(new TermQuery(new Term(fieldName, term)), Occur.MUST);            // MUST predstavlja AND operator
		}
		return builder.build();
	}

	private List<SearchHit> runQueryWithExplanation(String title, Query query) throws IOException {
		System.out.println();
		System.out.println(title);
		System.out.println("Objekat upita: " + query + " [" + query.getClass().getSimpleName() + "]");

		TopDocs hits = indexSearcher.search(query, BaseConfig.BROJ_REZULTATA_ZA_PRIKAZ);
		System.out.println("Broj pogodaka: " + hits.totalHits.value());

		StoredFields storedFields = indexReader.storedFields();
		List<SearchHit> searchHits = new ArrayList<>();
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document document = storedFields.document(scoreDoc.doc);
			SearchHit hit = new SearchHit(
					scoreDoc.doc,
					scoreDoc.score,
					document.get(BaseConfig.POLJE_NASLOV),
					document.get(BaseConfig.POLJE_VELICINA),
					document.get(BaseConfig.POLJE_PUTANJA));
			searchHits.add(hit);
			printHit(hit);
			Explanation explanation = indexSearcher.explain(query, scoreDoc.doc);        // razlaganje score vrednosti za odbranu
			System.out.println("Explanation:");
			System.out.println(explanation);
		}
		return searchHits;
	}

	private void compareScoresAndBoost(Query titleQuery, Query contentQuery, List<SearchHit> titleHits,
			List<SearchHit> contentHits) throws IOException {
		SearchHit titleHit = findCommonHit(titleHits, contentHits);
		SearchHit contentHit = findHitByTitle(contentHits, titleHit == null ? null : titleHit.title());
		if (titleHit == null || contentHit == null) {
			System.out.println();
			System.out.println("Nema zajednickog dokumenta za poredjenje score vrednosti.");
			return;
		}

		System.out.println();
		System.out.println("Poredjenje score vrednosti za zajednicki dokument");
		System.out.println("Dokument: " + titleHit.title());
		System.out.println("Score naslov: " + BaseConfig.formatScore(titleHit.score()));
		System.out.println("Score sadrzaj: " + BaseConfig.formatScore(contentHit.score()));

		Query boostedTitleQuery = titleQuery;
		Query boostedContentQuery = contentQuery;
		String boostedQueryLabel;
		float boost;

		if (titleHit.score() < contentHit.score()) {
			boost = contentHit.score() / titleHit.score();
			boostedTitleQuery = new BoostQuery(titleQuery, boost);                       // BoostQuery mnozi score unutrasnjeg upita
			boostedQueryLabel = "naslov";
		} else if (contentHit.score() < titleHit.score()) {
			boost = titleHit.score() / contentHit.score();
			boostedContentQuery = new BoostQuery(contentQuery, boost);
			boostedQueryLabel = "sadrzaj";
		} else {
			System.out.println("Score vrednosti su vec jednake, boost nije potreban.");
			return;
		}

		SearchHit boostedTitleHit = findHitByTitle(search(boostedTitleQuery), titleHit.title());
		SearchHit boostedContentHit = findHitByTitle(search(boostedContentQuery), titleHit.title());

		System.out.println("Boostovan upit: " + boostedQueryLabel + ", faktor: " + String.format(Locale.US, "%.6f", boost));
		System.out.println("Score naslov posle boost-a: " + BaseConfig.formatScore(boostedTitleHit.score()));
		System.out.println("Score sadrzaj posle boost-a: " + BaseConfig.formatScore(boostedContentHit.score()));
		System.out.println("Boostovani objekat upita: "
				+ ("naslov".equals(boostedQueryLabel) ? boostedTitleQuery : boostedContentQuery));
	}

	private List<SearchHit> search(Query query) throws IOException {
		TopDocs hits = indexSearcher.search(query, BaseConfig.BROJ_REZULTATA_ZA_PRIKAZ);
		StoredFields storedFields = indexReader.storedFields();
		List<SearchHit> searchHits = new ArrayList<>();
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document document = storedFields.document(scoreDoc.doc);
			searchHits.add(new SearchHit(
					scoreDoc.doc,
					scoreDoc.score,
					document.get(BaseConfig.POLJE_NASLOV),
					document.get(BaseConfig.POLJE_VELICINA),
					document.get(BaseConfig.POLJE_PUTANJA)));
		}
		return searchHits;
	}

	private static SearchHit findCommonHit(List<SearchHit> titleHits, List<SearchHit> contentHits) {
		SearchHit expected = findHitByTitle(titleHits, BaseConfig.OCEKIVANI_ZAJEDNICKI_DOKUMENT);
		if (expected != null && findHitByTitle(contentHits, expected.title()) != null) {
			return expected;
		}

		for (SearchHit titleHit : titleHits) {
			if (findHitByTitle(contentHits, titleHit.title()) != null) {
				return titleHit;
			}
		}
		return null;
	}

	private static SearchHit findHitByTitle(List<SearchHit> hits, String title) {
		if (title == null) {
			return null;
		}
		for (SearchHit hit : hits) {
			if (title.equals(hit.title())) {
				return hit;
			}
		}
		return null;
	}

	private static void printHit(SearchHit hit) {
		System.out.println(String.format(Locale.US, "score=%.6f | %s | %s B | %s",
				hit.score(),
				hit.title(),
				hit.size(),
				hit.path()));
	}

	@Override
	public void close() throws IOException {
		indexReader.close();
		directory.close();
	}

	private record SearchHit(int docId, float score, String title, String size, String path) {
	}
}
