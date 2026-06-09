import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher implements AutoCloseable {
	private static final String BOOLEAN_QUERY_TEXT = "(ghost OR wallpaper) AND NOT vampyre";
	private static final String TERM_RANGE_QUERY_TEXT = BaseConfig.POLJE_NAZIV_SORT
			+ ":[the_canterville_ghost TO the_vampyre_zzzz]";

	private final Analyzer analyzer;
	private final Directory directory;
	private final IndexReader indexReader;
	private final IndexSearcher indexSearcher;
	private final QueryParser parser;

	public static void main(String[] args) {
		try {
			runAllQueries();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public static void runAllQueries() throws Exception {
		runQueriesForIndex("TXT INDEKS", Path.of(BaseConfig.DIREKTORIJUM_TXT_INDEKS));
		runQueriesForIndex("TIKA INDEKS", Path.of(BaseConfig.DIREKTORIJUM_TIKA_INDEKS));
	}

	private static void runQueriesForIndex(String label, Path indexDirectory) throws Exception {
		System.out.println();
		System.out.println("==================================================");
		System.out.println(label + " - " + indexDirectory);
		System.out.println("==================================================");

		try (Searcher searcher = new Searcher(indexDirectory)) {
			searcher.runBooleanQueryPair();
			searcher.runTermRangeQueryPair();
		}
	}

	public Searcher(Path indexDirectory) throws IOException {
		this.analyzer = BaseConfig.createAnalyzer();
		this.directory = FSDirectory.open(indexDirectory);
		this.indexReader = DirectoryReader.open(directory);
		this.indexSearcher = new IndexSearcher(indexReader);
		this.parser = new QueryParser(BaseConfig.POLJE_SADRZAJ, analyzer);              // default polje parsera je sadrzaj
	}

	private void runBooleanQueryPair() throws Exception {
		Query objectQuery = createBooleanObjectQuery();
		runQuery("BooleanQuery - objektni model", objectQuery);
		runQuery("BooleanQuery - parser: " + BOOLEAN_QUERY_TEXT, parser.parse(BOOLEAN_QUERY_TEXT));
	}

	private Query createBooleanObjectQuery() {
		TermQuery ghost = new TermQuery(new Term(BaseConfig.POLJE_SADRZAJ, "ghost"));
		TermQuery wallpaper = new TermQuery(new Term(BaseConfig.POLJE_SADRZAJ, "wallpaper"));
		TermQuery vampyre = new TermQuery(new Term(BaseConfig.POLJE_SADRZAJ, "vampyre"));

		BooleanQuery.Builder orPart = new BooleanQuery.Builder();
		orPart.add(ghost, Occur.SHOULD);                                                // SHOULD + SHOULD predstavlja OR deo upita
		orPart.add(wallpaper, Occur.SHOULD);

		BooleanQuery.Builder query = new BooleanQuery.Builder();
		query.add(orPart.build(), Occur.MUST);                                          // MUST predstavlja AND uslov
		query.add(vampyre, Occur.MUST_NOT);                                             // MUST_NOT predstavlja NOT uslov
		return query.build();
	}

	private void runTermRangeQueryPair() throws Exception {
		Query objectQuery = TermRangeQuery.newStringRange(                              // indeks 19190 se zavrsava nulom: TermRangeQuery
				BaseConfig.POLJE_NAZIV_SORT,
				"the_canterville_ghost",
				"the_vampyre_zzzz",
				true,
				true);
		runQuery("TermRangeQuery - objektni model", objectQuery);
		runQuery("TermRangeQuery - parser: " + TERM_RANGE_QUERY_TEXT, parser.parse(TERM_RANGE_QUERY_TEXT));
	}

	private void runQuery(String title, Query query) throws IOException {
		System.out.println();
		System.out.println(title);
		System.out.println("Objekat upita: " + query + " [" + query.getClass().getSimpleName() + "]");

		TopDocs hits = indexSearcher.search(query, BaseConfig.BROJ_REZULTATA_ZA_PRIKAZ);
		System.out.println("Broj pogodaka: " + hits.totalHits.value());

		StoredFields storedFields = indexReader.storedFields();
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document document = storedFields.document(scoreDoc.doc);
			System.out.println(String.format("score=%.4f | %s | format=%s | %s B | %s",
					scoreDoc.score,
					document.get(BaseConfig.POLJE_NAZIV),
					document.get(BaseConfig.POLJE_FORMAT),
					document.get(BaseConfig.POLJE_VELICINA),
					document.get(BaseConfig.POLJE_PUTANJA)));
		}
	}

	@Override
	public void close() throws IOException {
		indexReader.close();
		directory.close();
		analyzer.close();
	}
}
