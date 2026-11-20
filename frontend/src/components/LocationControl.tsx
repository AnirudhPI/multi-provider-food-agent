import type { Coordinates } from '../api/types'

type LocationState =
  | { status: 'missing' }
  | { status: 'locating' }
  | { status: 'ready'; coordinates: Coordinates }
  | { status: 'error'; message: string }

type Props = {
  location: LocationState
  onRequest: () => void
  onClear: () => void
}

export type { LocationState }

export function LocationControl({ location, onRequest, onClear }: Props) {
  const ready = location.status === 'ready'
  return (
    <div className={`location-pill ${ready ? 'location-pill--ready' : ''}`}>
      <span className="location-dot" aria-hidden="true" />
      <div>
        <span className="location-label">
          {location.status === 'locating'
            ? 'Finding location…'
            : ready
              ? 'Location ready'
              : location.status === 'error'
                ? 'Location unavailable'
                : 'Location not shared'}
        </span>
        {location.status === 'error' && <span className="location-error">{location.message}</span>}
      </div>
      <button type="button" className="location-action" onClick={ready ? onClear : onRequest} disabled={location.status === 'locating'}>
        {ready ? 'Clear' : 'Use my location'}
      </button>
    </div>
  )
}
