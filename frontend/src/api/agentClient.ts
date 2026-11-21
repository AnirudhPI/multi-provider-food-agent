import type {
  AgentStreamError,
  AgentTurnRequest,
  AgentTurnResponse,
  TraceEvent,
} from './types'

type StreamHandlers = {
  onTrace: (event: TraceEvent) => void
}

type SseMessage = {
  event: string
  data: string
}

export class AgentApiError extends Error {
  readonly code: string

  constructor(error: AgentStreamError) {
    super(error.message)
    this.name = 'AgentApiError'
    this.code = error.code
  }
}

export async function streamAgentTurn(
  request: AgentTurnRequest,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<AgentTurnResponse> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
  const response = await fetch(`${baseUrl}/api/v1/agent/turns/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(request),
    signal,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      code: 'HTTP_ERROR',
      message: `The backend returned HTTP ${response.status}.`,
    }))
    throw new AgentApiError(error as AgentStreamError)
  }
  if (!response.body) {
    throw new AgentApiError({ code: 'EMPTY_STREAM', message: 'The backend returned an empty response.' })
  }

  let finalResult: AgentTurnResponse | undefined
  let streamError: AgentStreamError | undefined
  await readSse(response.body, (message) => {
    if (message.event === 'trace') {
      handlers.onTrace(JSON.parse(message.data) as TraceEvent)
    } else if (message.event === 'result') {
      finalResult = JSON.parse(message.data) as AgentTurnResponse
    } else if (message.event === 'error') {
      streamError = JSON.parse(message.data) as AgentStreamError
    }
  })

  if (streamError) {
    throw new AgentApiError(streamError)
  }
  if (!finalResult) {
    throw new AgentApiError({
      code: 'INCOMPLETE_STREAM',
      message: 'The agent stream ended before a final result arrived.',
    })
  }
  return finalResult
}

async function readSse(
  stream: ReadableStream<Uint8Array>,
  onMessage: (message: SseMessage) => void,
): Promise<void> {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value, { stream: !done }).replaceAll('\r\n', '\n')
    const frames = buffer.split('\n\n')
    buffer = frames.pop() ?? ''
    for (const frame of frames) {
      const parsed = parseFrame(frame)
      if (parsed) onMessage(parsed)
    }
    if (done) break
  }

  const finalFrame = parseFrame(buffer)
  if (finalFrame) onMessage(finalFrame)
}

function parseFrame(frame: string): SseMessage | undefined {
  if (!frame.trim()) return undefined
  let event = 'message'
  const data: string[] = []
  for (const line of frame.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
  }
  return data.length ? { event, data: data.join('\n') } : undefined
}
