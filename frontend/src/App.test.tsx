import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const successfulStream = [
  'event:trace\ndata:{"turnId":"turn-1","type":"TURN_STARTED","step":0,"toolName":null,"occurredAt":"2026-11-20T10:00:00Z"}\n\n',
  'event:trace\ndata:{"turnId":"turn-1","type":"TOOL_COMPLETED","step":1,"toolName":"search_restaurants","occurredAt":"2026-11-20T10:00:01Z"}\n\n',
  'event:result\ndata:{"turnId":"turn-1","outcome":"COMPLETED","message":"I found a good dosa option.","restaurantSearches":[{"provider":"SWIGGY","query":"dosa","restaurants":[{"providerRestaurantId":"one","name":"Learning Dosa House","cuisines":["South Indian"],"rating":4.4,"totalRatingsDisplay":"1K+","costForTwoDisplay":"₹250 for two","areaName":"Central","distanceKm":2.1,"deliveryTimeMinutes":25,"vegetarian":true,"offer":null,"imageUrl":null,"availabilityStatus":"OPEN"}],"nextContinuationToken":null,"hasMore":false,"observedAt":"2026-11-20T10:00:01Z"}],"trace":[]}\n\n',
]

describe('App', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('submits a natural-language request and renders grounded restaurant results', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(streamResponse(successfulStream)))
    const user = userEvent.setup()
    render(<App />)

    await user.type(screen.getByLabelText(/what food would you like/i), 'Find vegetarian dosa')
    await user.click(screen.getByRole('button', { name: /send request/i }))

    expect(await screen.findByText('Learning Dosa House')).toBeInTheDocument()
    expect(screen.getByText('I found a good dosa option.')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/agent/turns/stream',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('requests browser location without exposing coordinates in the UI', async () => {
    const getCurrentPosition = vi.fn((success) => success({ coords: { latitude: 12.9, longitude: 77.6 } }))
    vi.stubGlobal('navigator', { ...navigator, geolocation: { getCurrentPosition } })
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /use my location/i }))

    expect(await screen.findByText('Location ready')).toBeInTheDocument()
    expect(screen.queryByText('12.9')).not.toBeInTheDocument()
  })

  it('shows an actionable provider authentication error and restores the request', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(streamResponse([
      'event:error\ndata:{"code":"PROVIDER_NOT_AUTHENTICATED","message":"expired"}\n\n',
    ])))
    const user = userEvent.setup()
    render(<App />)

    const input = screen.getByLabelText(/what food would you like/i)
    await user.type(input, 'Find dosa')
    await user.click(screen.getByRole('button', { name: /send request/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Swiggy authentication has expired')
    await waitFor(() => expect(input).toHaveValue('Find dosa'))
  })
})

function streamResponse(chunks: string[]): Response {
  const encoder = new TextEncoder()
  return new Response(new ReadableStream({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(encoder.encode(chunk))
      controller.close()
    },
  }), { status: 200, headers: { 'Content-Type': 'text/event-stream' } })
}
