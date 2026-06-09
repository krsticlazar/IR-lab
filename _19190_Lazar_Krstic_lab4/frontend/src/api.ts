import type { FieldOption, IndexStatus, IndexSummary, SearchResponse } from './types';

const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:5044';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function getDownloadUrl(path: string) {
  return `${API_BASE_URL}${path}`;
}

export function getFields() {
  return request<FieldOption[]>('/api/fields');
}

export function getIndexStatus() {
  return request<IndexStatus>('/api/index/status');
}

export function rebuildIndex() {
  return request<IndexSummary>('/api/index/rebuild', { method: 'POST' });
}

export function searchDocuments(field: string, query: string, page: number) {
  return request<SearchResponse>('/api/search', {
    method: 'POST',
    body: JSON.stringify({ field, query, page }),
  });
}
