import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, DatabaseZap, Download, RefreshCw, Search } from 'lucide-react';
import { getDownloadUrl, getFields, getIndexStatus, rebuildIndex, searchDocuments } from './api';
import type { FieldOption, IndexStatus, SearchResponse } from './types';

const defaultField = 'content';

export default function App() {
  const [fields, setFields] = useState<FieldOption[]>([]);
  const [status, setStatus] = useState<IndexStatus | null>(null);
  const [field, setField] = useState(defaultField);
  const [query, setQuery] = useState('ghost');
  const [searchResponse, setSearchResponse] = useState<SearchResponse | null>(null);
  const [loadingIndex, setLoadingIndex] = useState(false);
  const [loadingSearch, setLoadingSearch] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    void loadInitialData();
  }, []);

  const pages = useMemo(() => {
    if (!searchResponse || searchResponse.totalPages === 0) {
      return [];
    }

    return Array.from({ length: searchResponse.totalPages }, (_, index) => index + 1);
  }, [searchResponse]);

  async function loadInitialData() {
    try {
      setError('');
      const [fieldOptions, indexStatus] = await Promise.all([getFields(), getIndexStatus()]);
      setFields(fieldOptions);
      setStatus(indexStatus);
      if (fieldOptions.length > 0) {
        setField(fieldOptions[0].value);
      }
    } catch (caught) {
      setError(toErrorMessage(caught));
    }
  }

  async function handleRebuildIndex() {
    try {
      setError('');
      setMessage('');
      setLoadingIndex(true);
      const summary = await rebuildIndex();
      setMessage(`Indeksirano dokumenata: ${summary.indexedDocuments}`);
      const indexStatus = await getIndexStatus();
      setStatus(indexStatus);
      if (query.trim()) {
        await runSearch(1);
      }
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoadingIndex(false);
    }
  }

  async function runSearch(page: number) {
    try {
      setError('');
      setLoadingSearch(true);
      const response = await searchDocuments(field, query, page);
      setSearchResponse(response);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoadingSearch(false);
    }
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void runSearch(1);
  }

  return (
    <main className="app-shell">
      <section className="toolbar" aria-label="Indeksiranje">
        <div>
          <h1>IR Lab 4</h1>
          <p>{statusText(status)}</p>
        </div>
        <button className="primary-button" type="button" onClick={handleRebuildIndex} disabled={loadingIndex}>
          <DatabaseZap size={18} />
          {loadingIndex ? 'Indeksiranje...' : 'Indeksiraj'}
        </button>
      </section>

      <form className="search-panel" onSubmit={handleSubmit}>
        <label>
          <span>Polje</span>
          <select value={field} onChange={(event) => setField(event.target.value)}>
            {fields.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Upit</span>
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="anna karenina" />
        </label>

        <button className="secondary-button" type="submit" disabled={loadingSearch}>
          <Search size={18} />
          {loadingSearch ? 'Pretraga...' : 'Trazi'}
        </button>

        <button className="icon-button" type="button" onClick={() => void loadInitialData()} title="Osvezi status">
          <RefreshCw size={18} />
        </button>
      </form>

      {message && <p className="message">{message}</p>}
      {error && <p className="error">{error}</p>}

      {searchResponse && (
        <section className="results-section" aria-label="Rezultati">
          <div className="results-summary">
            <strong>Pronadjeno je ukupno {searchResponse.total} rezultata</strong>
            <span>
              Strana {searchResponse.totalPages === 0 ? 0 : searchResponse.page} od {searchResponse.totalPages}
            </span>
          </div>

          <div className="results-list">
            {searchResponse.results.map((result, index) => (
              <article className="result-item" key={result.id}>
                <div className="result-title-row">
                  <span>Rezultat {(searchResponse.page - 1) * searchResponse.pageSize + index + 1}</span>
                  <a href={getDownloadUrl(result.downloadUrl)} target="_blank" rel="noreferrer">
                    {result.fileName}
                  </a>
                  <a className="download-link" href={getDownloadUrl(result.downloadUrl)} download>
                    <Download size={16} />
                    Preuzmi
                  </a>
                </div>
                <dl>
                  <div>
                    <dt>Velicina dokumenta</dt>
                    <dd>{result.sizeText}</dd>
                  </div>
                  <div>
                    <dt>Datum poslednje izmene</dt>
                    <dd>{result.modifiedAtText}</dd>
                  </div>
                  <div>
                    <dt>Datum indeksiranja</dt>
                    <dd>{result.indexedAtText}</dd>
                  </div>
                  <div>
                    <dt>Score</dt>
                    <dd>{result.score?.toFixed(4) ?? 'n/a'}</dd>
                  </div>
                </dl>
                <p>{result.preview}</p>
              </article>
            ))}
          </div>

          {pages.length > 1 && (
            <nav className="pagination" aria-label="Stranice">
              <button
                type="button"
                onClick={() => void runSearch(searchResponse.page - 1)}
                disabled={searchResponse.page <= 1}
              >
                <ChevronLeft size={16} />
              </button>
              {pages.map((page) => (
                <button
                  className={page === searchResponse.page ? 'active-page' : ''}
                  key={page}
                  type="button"
                  onClick={() => void runSearch(page)}
                >
                  {page}
                </button>
              ))}
              <button
                type="button"
                onClick={() => void runSearch(searchResponse.page + 1)}
                disabled={searchResponse.page >= searchResponse.totalPages}
              >
                <ChevronRight size={16} />
              </button>
            </nav>
          )}
        </section>
      )}
    </main>
  );
}

function statusText(status: IndexStatus | null) {
  if (!status) {
    return 'Status indeksa nije ucitan.';
  }

  return status.exists
    ? `${status.indexName}: ${status.documentCount} dokumenata`
    : `${status.indexName}: indeks jos nije kreiran`;
}

function toErrorMessage(caught: unknown) {
  return caught instanceof Error ? caught.message : 'Doslo je do greske.';
}
