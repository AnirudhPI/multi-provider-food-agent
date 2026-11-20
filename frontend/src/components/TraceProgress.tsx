import type { TraceEvent } from '../api/types'

const labels: Record<TraceEvent['type'], string> = {
  TURN_STARTED: 'Starting your request',
  MODEL_REQUESTED: 'Understanding what you need',
  TOOL_STARTED: 'Searching Swiggy',
  TOOL_COMPLETED: 'Swiggy results received',
  TOOL_FAILED: 'Swiggy search could not complete',
  TURN_COMPLETED: 'Response ready',
  CLARIFICATION_REQUIRED: 'More information needed',
  UNSUPPORTED: 'Request is outside current capabilities',
}

type Props = {
  events: TraceEvent[]
}

export function TraceProgress({ events }: Props) {
  const latest = events.at(-1)
  return (
    <div className="progress-card" role="status" aria-live="polite">
      <div className="progress-orb" aria-hidden="true"><span /></div>
      <div>
        <p>{latest ? labels[latest.type] : 'Connecting to your local agent'}</p>
        <span>Private model · read-only search</span>
      </div>
    </div>
  )
}
