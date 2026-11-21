import { type FormEvent, useRef, useState } from 'react'
import { AgentApiError, streamAgentTurn } from './api/agentClient'
import type { AgentTurnResponse, Coordinates, TraceEvent } from './api/types'
import { LocationControl, type LocationState } from './components/LocationControl'
import { RestaurantCard } from './components/RestaurantCard'
import { TraceProgress } from './components/TraceProgress'

type Exchange = {
  id: string
  prompt: string
  response: AgentTurnResponse
}

const suggestions = [
  'Find highly rated vegetarian dosa',
  'Show quick delivery options nearby',
  'Find an affordable South Indian breakfast',
]

function App() {
  const [message, setMessage] = useState('')
  const [location, setLocation] = useState<LocationState>({ status: 'missing' })
  const [history, setHistory] = useState<Exchange[]>([])
  const [activePrompt, setActivePrompt] = useState<string | null>(null)
  const [trace, setTrace] = useState<TraceEvent[]>([])
  const [error, setError] = useState<string | null>(null)
  const abortController = useRef<AbortController | null>(null)

  const requestLocation = () => {
    if (!navigator.geolocation) {
      setLocation({ status: 'error', message: 'This browser does not provide geolocation.' })
      return
    }
    setLocation({ status: 'locating' })
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => setLocation({
        status: 'ready',
        coordinates: { latitude: coords.latitude, longitude: coords.longitude },
      }),
      () => setLocation({ status: 'error', message: 'Permission was denied. You can still ask a question.' }),
      { enableHighAccuracy: false, timeout: 10_000, maximumAge: 300_000 },
    )
  }

  const submit = async (event?: FormEvent) => {
    event?.preventDefault()
    const prompt = message.trim()
    if (!prompt || activePrompt) return

    setError(null)
    setTrace([])
    setActivePrompt(prompt)
    setMessage('')
    const controller = new AbortController()
    abortController.current = controller
    const coordinates: Coordinates | undefined = location.status === 'ready' ? location.coordinates : undefined

    try {
      const response = await streamAgentTurn(
        { message: prompt, ...(coordinates ? { location: coordinates } : {}) },
        { onTrace: (event) => setTrace((current) => [...current, event]) },
        controller.signal,
      )
      setHistory((current) => [...current, { id: response.turnId, prompt, response }])
    } catch (caught) {
      if ((caught as Error).name !== 'AbortError') {
        setError(friendlyError(caught))
        setMessage(prompt)
      }
    } finally {
      setActivePrompt(null)
      abortController.current = null
    }
  }

  const useSuggestion = (suggestion: string) => {
    setMessage(suggestion)
    document.querySelector<HTMLTextAreaElement>('#food-request')?.focus()
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#top" aria-label="Food Agent home">
          <span className="brand-mark" aria-hidden="true">F</span>
          <span>Food Agent</span>
        </a>
        <div className="topbar-actions">
          <span className="local-status"><i /> Ollama local</span>
          {history.length > 0 && (
            <button type="button" className="quiet-button" onClick={() => setHistory([])}>Clear conversation</button>
          )}
        </div>
      </header>

      <main id="top">
        <section className="hero">
          <p className="eyebrow">Private · local-first · provider-grounded</p>
          <h1>What are you hungry for?</h1>
          <p className="hero-copy">Describe the food, budget, or mood. Your local model turns it into a safe, read-only Swiggy search.</p>

          <LocationControl
            location={location}
            onRequest={requestLocation}
            onClear={() => setLocation({ status: 'missing' })}
          />

          {history.length === 0 && !activePrompt && (
            <div className="suggestions" aria-label="Example requests">
              {suggestions.map((suggestion) => (
                <button key={suggestion} type="button" onClick={() => useSuggestion(suggestion)}>{suggestion}</button>
              ))}
            </div>
          )}
        </section>

        <section className="conversation" aria-label="Conversation">
          {history.map(({ id, prompt, response }) => (
            <div className="exchange" key={id}>
              <div className="user-message"><span>You</span><p>{prompt}</p></div>
              <div className="assistant-message">
                <div className="assistant-icon" aria-hidden="true">F</div>
                <div className="assistant-content">
                  <span className="speaker">Food Agent</span>
                  <p className="assistant-copy">{response.message}</p>
                  {response.restaurantSearches.map((search) => (
                    <div key={`${id}-${search.provider}-${search.query}`}>
                      <div className="result-heading">
                        <h2>{search.restaurants.length} {search.restaurants.length === 1 ? 'place' : 'places'} from {titleCase(search.provider)}</h2>
                        <span>Observed {formatTime(search.observedAt)}</span>
                      </div>
                      <div className="restaurant-grid">
                        {search.restaurants.map((restaurant, index) => (
                          <RestaurantCard
                            key={restaurant.providerRestaurantId ?? `${restaurant.name}-${index}`}
                            restaurant={restaurant}
                            provider={titleCase(search.provider)}
                          />
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ))}

          {activePrompt && (
            <div className="exchange">
              <div className="user-message"><span>You</span><p>{activePrompt}</p></div>
              <TraceProgress events={trace} />
            </div>
          )}

          {error && (
            <div className="error-card" role="alert">
              <div><strong>That search did not complete.</strong><p>{error}</p></div>
              <button type="button" onClick={() => setError(null)}>Dismiss</button>
            </div>
          )}
        </section>
      </main>

      <div className="composer-dock">
        <form className="composer" onSubmit={submit}>
          <label htmlFor="food-request" className="sr-only">What food would you like to find?</label>
          <textarea
            id="food-request"
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault()
                void submit()
              }
            }}
            placeholder="Ask for a dish, cuisine, budget, or delivery preference…"
            maxLength={2000}
            rows={1}
            disabled={Boolean(activePrompt)}
          />
          {activePrompt ? (
            <button
              type="button"
              className="send-button send-button--stop"
              aria-label="Stop waiting for this request"
              onClick={() => abortController.current?.abort()}
            >■</button>
          ) : (
            <button type="submit" className="send-button" aria-label="Send request" disabled={!message.trim()}>↑</button>
          )}
        </form>
        <p className="composer-note">Read-only discovery. Provider details may change; verify them before making a decision.</p>
      </div>
    </div>
  )
}

function friendlyError(caught: unknown): string {
  if (caught instanceof AgentApiError) {
    const messages: Record<string, string> = {
      MODEL_UNAVAILABLE: 'Ollama is not reachable. Start Ollama and try again.',
      MODEL_TIMEOUT: 'The local model took too long to respond. Please try a shorter request.',
      AGENT_TIMEOUT: 'The complete search exceeded its time limit. Please try again.',
      PROVIDER_NOT_AUTHENTICATED: 'Swiggy authentication has expired. Sign in again before retrying.',
      PROVIDER_UNAVAILABLE: 'Swiggy is not available from the backend right now.',
      PROVIDER_TIMEOUT: 'Swiggy took too long to respond. Please try again.',
      INVALID_REQUEST: 'The request could not be validated. Check it and try again.',
    }
    return messages[caught.code] ?? caught.message
  }
  return 'The backend could not be reached. Confirm that it is running on port 8080.'
}

function titleCase(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

function formatTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.valueOf()) ? 'recently' : date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
}

export default App
