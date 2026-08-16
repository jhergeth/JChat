<script setup>
import { onMounted, onUnmounted, ref, computed, watch } from 'vue'

const conversationId = ref('default')
const autoRefresh = ref(true)
const hideMetaRequests = ref(true)
const followLatestChat = ref(true)
const loading = ref(false)
const error = ref(null)
const traces = ref([])
const selectedTraceId = ref(null)
const knowledgeStore = ref([])

let timer = null

const snapshot = computed(() => {
  if (!traces.value.length) return null
  if (selectedTraceId.value) {
    return traces.value.find(t => t.id === selectedTraceId.value) ?? traces.value[0]
  }
  return traces.value[0]
})

const visibleTraces = computed(() =>
  hideMetaRequests.value
    ? traces.value.filter(t => t.requestType === 'chat')
    : traces.value
)

const retrievedContext = computed(() => snapshot.value?.retrievedContext ?? [])
const searchTrace = computed(() => snapshot.value?.searchTrace ?? null)
const prompt = computed(() => snapshot.value?.prompt ?? [])

const searchHistory = computed(() =>
  traces.value
    .filter(t => t.searchTrace?.searched)
    .map(t => ({
      id: t.id,
      timestamp: t.timestamp,
      userInput: t.userInput,
      query: t.searchTrace.query,
      status: t.searchTrace.status,
      snippetCount: t.searchTrace.snippetCount,
      tripleCount: t.searchTrace.extractedTriples?.length ?? 0,
    }))
)

const requestTypeLabel = {
  chat: 'Chat',
  follow_up: 'Follow-up',
  meta: 'Meta',
}

function previewInput(text) {
  if (!text) return '—'
  const oneLine = text.replace(/\s+/g, ' ').trim()
  if (oneLine.startsWith('### Task:')) {
    const match = oneLine.match(/### Task:\s*(.{0,60})/)
    return match ? `Task: ${match[1]}…` : 'Open WebUI Meta-Request'
  }
  return oneLine.length <= 72 ? oneLine : oneLine.slice(0, 72) + '…'
}

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const conv = conversationId.value.trim() || 'default'
    const params = new URLSearchParams({
      limit: '30',
      conversationId: conv,
      includeMeta: 'true',
    })
    const tracesUrl = `/api/debug/traces?${params}`
    const storeUrl = `/api/debug/knowledge-store?conversationId=${encodeURIComponent(conv)}`

    const [tracesRes, storeRes] = await Promise.all([fetch(tracesUrl), fetch(storeUrl)])

    if (!tracesRes.ok) {
      throw new Error(`Traces: HTTP ${tracesRes.status}`)
    }
    const allTraces = await tracesRes.json()
    traces.value = allTraces

    if (followLatestChat.value) {
      const latestChat = allTraces.find(t => t.requestType === 'chat')
      selectedTraceId.value = latestChat?.id ?? allTraces[0]?.id ?? null
    } else if (selectedTraceId.value && !allTraces.some(t => t.id === selectedTraceId.value)) {
      selectedTraceId.value = allTraces[0]?.id ?? null
    }

    if (!storeRes.ok) {
      throw new Error(`Knowledge store: HTTP ${storeRes.status}`)
    }
    const storeData = await storeRes.json()
    knowledgeStore.value = storeData.statements ?? []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function selectTrace(trace) {
  followLatestChat.value = false
  selectedTraceId.value = trace.id
}

function startPolling() {
  stopPolling()
  if (autoRefresh.value) {
    timer = setInterval(fetchData, 2000)
  }
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(autoRefresh, startPolling)
watch(hideMetaRequests, () => {
  followLatestChat.value = true
  fetchData()
})

onMounted(() => {
  fetchData()
  startPolling()
})

onUnmounted(stopPolling)

function formatTime(iso) {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return String(iso)
  return date.toLocaleString('de-DE', {
    timeZone: 'Europe/Berlin',
    dateStyle: 'short',
    timeStyle: 'medium',
  })
}

function badgeClass(type) {
  return `badge badge-${type ?? 'chat'}`
}
</script>

<template>
  <div class="page">
    <header class="header">
      <h1>JChat Debug</h1>
      <p class="subtitle">Turn-Historie — Eingabe, Kontext, Prompt, Antwort, Knowledge Store</p>

      <div class="controls">
        <label>
          Conversation ID
          <input v-model="conversationId" type="text" placeholder="default" @keyup.enter="fetchData" />
        </label>
        <button type="button" @click="fetchData" :disabled="loading">Aktualisieren</button>
        <label class="checkbox">
          <input v-model="autoRefresh" type="checkbox" />
          Auto-Refresh (2s)
        </label>
        <label class="checkbox">
          <input v-model="hideMetaRequests" type="checkbox" />
          Meta-Requests ausblenden
        </label>
        <label class="checkbox">
          <input v-model="followLatestChat" type="checkbox" />
          Neuesten Chat-Turn verfolgen
        </label>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="snapshot" class="meta">
        {{ formatTime(snapshot.timestamp) }}
        · Conversation: {{ snapshot.conversationId }}
        · Provider: {{ snapshot.chatProvider }}
        <span :class="badgeClass(snapshot.requestType)">{{ requestTypeLabel[snapshot.requestType] ?? snapshot.requestType }}</span>
      </p>
    </header>

    <div class="layout">
      <aside class="trace-list panel">
        <h2>Turns</h2>
        <p v-if="!visibleTraces.length" class="empty">Noch keine Turns.</p>
        <button
          v-for="trace in visibleTraces"
          :key="trace.id"
          type="button"
          class="trace-item"
          :class="{ active: trace.id === selectedTraceId }"
          @click="selectTrace(trace)"
        >
          <span class="trace-time">{{ formatTime(trace.timestamp) }}</span>
          <span :class="badgeClass(trace.requestType)">{{ requestTypeLabel[trace.requestType] ?? trace.requestType }}</span>
          <span v-if="trace.searchTrace?.searched" class="badge badge-search" title="Websuche">🔍</span>
          <span class="trace-preview">{{ previewInput(trace.userInput) }}</span>
        </button>
      </aside>

      <main class="content">
        <section class="panel">
          <h2>User-Eingabe</h2>
          <pre v-if="snapshot?.userInput" class="text-block">{{ snapshot.userInput }}</pre>
          <p v-else class="empty">Noch keine Anfrage — chatte über Open WebUI oder curl.</p>
        </section>

        <section class="panel">
          <h2>Aktueller Kontext</h2>
          <template v-if="snapshot?.ambientContext">
            <p><strong>Datum/Uhrzeit:</strong> {{ snapshot.ambientContext.localDateTime }} ({{ snapshot.ambientContext.timezone }})</p>
            <p><strong>Tageszeit:</strong> {{ snapshot.ambientContext.dayPhase }}</p>
            <p><strong>Locale:</strong> {{ snapshot.ambientContext.locale || '—' }}</p>
            <p><strong>Vermutetes Land:</strong> {{ snapshot.ambientContext.country }}</p>
          </template>
          <p v-else class="empty">Kein Session-Kontext für diesen Turn.</p>
        </section>

        <section class="panel">
          <h2>Kontext (Retriever)</h2>
          <ul v-if="retrievedContext.length">
            <li v-for="(line, i) in retrievedContext" :key="i"><code>{{ line }}</code></li>
          </ul>
          <p v-else class="empty">Kein retrieved Context für diesen Turn.</p>
        </section>

        <section class="panel">
          <h2>Websuche-Historie</h2>
          <p v-if="!searchHistory.length" class="empty">Noch keine Websuchen in dieser Conversation.</p>
          <ul v-else class="search-history">
            <li v-for="entry in searchHistory" :key="entry.id">
              <button type="button" class="search-history-item" @click="selectTrace(traces.find(t => t.id === entry.id))">
                <span class="trace-time">{{ formatTime(entry.timestamp) }}</span>
                <code>{{ entry.query }}</code>
                <span class="search-meta">{{ entry.status }} · {{ entry.snippetCount }} Snippets · {{ entry.tripleCount }} Triples</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="panel">
          <h2>Web-Suche (aktueller Turn)</h2>
          <template v-if="searchTrace?.searched">
            <p><strong>Status:</strong> {{ searchTrace.status }} — {{ searchTrace.detail }}</p>
            <p><strong>Query:</strong> <code>{{ searchTrace.query }}</code></p>
            <p><strong>Snippets:</strong> {{ searchTrace.snippetCount }}</p>
            <p v-if="searchTrace.promptContext"><strong>Prompt-Kontext:</strong></p>
            <pre v-if="searchTrace.promptContext" class="text-block">{{ searchTrace.promptContext }}</pre>
            <div v-if="searchTrace.snippets?.length" class="snippet-list">
              <article v-for="(snippet, i) in searchTrace.snippets" :key="'s' + i" class="snippet-card">
                <h3>{{ snippet.title || `Treffer ${i + 1}` }}</h3>
                <a v-if="snippet.url" :href="snippet.url" target="_blank" rel="noopener">{{ snippet.url }}</a>
                <pre class="text-block snippet-preview">{{ snippet.text }}</pre>
              </article>
            </div>
            <p v-else class="empty">Keine Snippet-Details verfügbar.</p>
            <template v-if="searchTrace.extractedTriples?.length">
              <p><strong>Extrahierte Triples:</strong></p>
              <ul>
                <li v-for="(line, i) in searchTrace.extractedTriples" :key="i"><code>{{ line }}</code></li>
              </ul>
            </template>
            <p v-else-if="searchTrace?.detail?.includes('folgt nach')" class="search-hint">
              Triple-Extraktion läuft im Hintergrund (nach der Antwort) — Auto-Refresh zeigt sie gleich.
            </p>
            <p v-else-if="searchTrace?.promptContext" class="search-hint">
              Keine RDF-Triples — die Recherche wurde als Textauszüge in den System-Prompt übernommen (siehe „Prompt-Kontext“).
            </p>
            <p v-else class="empty">Keine Triples aus Snippets extrahiert.</p>
          </template>
          <template v-else-if="searchTrace">
            <p class="search-status"><strong>{{ searchTrace.status }}:</strong> {{ searchTrace.detail }}</p>
          </template>
          <p v-else class="empty">Keine Websuche für diesen Turn.</p>
        </section>

        <section class="panel">
          <h2>Erzeugter Prompt</h2>
          <div v-if="prompt.length">
            <div v-for="(line, i) in prompt" :key="i" class="prompt-line">
              <div class="prompt-role">{{ line.role }}</div>
              <pre class="text-block">{{ line.content }}</pre>
            </div>
          </div>
          <p v-else class="empty">—</p>
        </section>

        <section class="panel">
          <h2>LLM-Antwort</h2>
          <pre v-if="snapshot?.llmResponse" class="text-block">{{ snapshot.llmResponse }}</pre>
          <p v-else class="empty">—</p>
        </section>

        <section class="panel">
          <h2>Knowledge Store</h2>
          <table v-if="knowledgeStore.length">
            <thead>
              <tr>
                <th>Subject</th>
                <th>Predicate</th>
                <th>Object</th>
                <th>Turn</th>
                <th>Zeit</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in knowledgeStore" :key="i">
                <td>{{ s.subject }}</td>
                <td>{{ s.predicate }}</td>
                <td>{{ s.object }}</td>
                <td><code>{{ s.turnId?.slice(0, 8) }}</code></td>
                <td>{{ formatTime(s.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty">Store leer für diese Conversation.</p>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem;
}

.header h1 {
  margin: 0 0 0.25rem;
  font-size: 1.5rem;
}

.subtitle {
  margin: 0 0 1rem;
  color: #71717a;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1rem;
  align-items: end;
  margin-bottom: 0.75rem;
}

.controls label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.875rem;
}

.controls input[type='text'] {
  padding: 0.4rem 0.6rem;
  border: 1px solid #d4d4d8;
  border-radius: 6px;
  min-width: 200px;
}

.controls button {
  padding: 0.45rem 0.9rem;
  border: 1px solid #d4d4d8;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.controls button:disabled {
  opacity: 0.6;
}

.checkbox {
  flex-direction: row !important;
  align-items: center;
}

.error {
  color: #b91c1c;
  margin: 0.5rem 0 0;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 1rem;
  margin-top: 1rem;
}

@media (max-width: 800px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.trace-list {
  max-height: calc(100vh - 12rem);
  overflow-y: auto;
}

.trace-list h2 {
  margin-top: 0;
  font-size: 1rem;
}

.trace-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  width: 100%;
  margin-bottom: 0.5rem;
  padding: 0.5rem 0.6rem;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
  cursor: pointer;
  text-align: left;
}

.trace-item:hover {
  background: #f4f4f5;
}

.trace-item.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.trace-time {
  font-size: 0.75rem;
  color: #71717a;
}

.trace-preview {
  font-size: 0.8125rem;
  color: #27272a;
}

.badge {
  display: inline-block;
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
}

.badge-chat {
  background: #dcfce7;
  color: #166534;
}

.badge-follow_up {
  background: #fef9c3;
  color: #854d0e;
}

.badge-meta {
  background: #f3e8ff;
  color: #6b21a8;
}

.badge-search {
  background: #dbeafe;
  color: #1d4ed8;
}

.search-history {
  list-style: none;
  margin: 0;
  padding: 0;
}

.search-history-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  width: 100%;
  margin-bottom: 0.5rem;
  padding: 0.5rem 0.6rem;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
  cursor: pointer;
  text-align: left;
}

.search-history-item:hover {
  background: #eff6ff;
}

.search-meta {
  font-size: 0.75rem;
  color: #71717a;
}

.snippet-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin: 0.75rem 0;
}

.snippet-card {
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  padding: 0.75rem;
  background: #fafafa;
}

.snippet-card h3 {
  margin: 0 0 0.25rem;
  font-size: 0.9375rem;
}

.snippet-card a {
  font-size: 0.75rem;
  color: #2563eb;
  word-break: break-all;
}

.snippet-preview {
  margin-top: 0.5rem;
  max-height: 12rem;
  overflow-y: auto;
  font-size: 0.8125rem;
}

.content {
  min-width: 0;
}
</style>
