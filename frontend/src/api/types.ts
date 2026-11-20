export type Coordinates = {
  latitude: number
  longitude: number
}

export type AgentTurnRequest = {
  message: string
  location?: Coordinates
}

export type TraceEvent = {
  turnId: string
  type:
    | 'TURN_STARTED'
    | 'MODEL_REQUESTED'
    | 'TOOL_STARTED'
    | 'TOOL_COMPLETED'
    | 'TOOL_FAILED'
    | 'TURN_COMPLETED'
    | 'CLARIFICATION_REQUIRED'
    | 'UNSUPPORTED'
  step: number
  toolName: string | null
  occurredAt: string
}

export type Restaurant = {
  providerRestaurantId: string | null
  name: string
  cuisines: string[]
  rating: number | null
  totalRatingsDisplay: string | null
  costForTwoDisplay: string | null
  areaName: string | null
  distanceKm: number | null
  deliveryTimeMinutes: number | null
  vegetarian: boolean | null
  offer: string | null
  imageUrl: string | null
  availabilityStatus: string | null
}

export type RestaurantSearch = {
  provider: string
  query: string
  restaurants: Restaurant[]
  nextContinuationToken: string | null
  hasMore: boolean
  observedAt: string
}

export type AgentTurnResponse = {
  turnId: string
  outcome: 'COMPLETED' | 'CLARIFICATION_REQUIRED' | 'UNSUPPORTED'
  message: string
  restaurantSearches: RestaurantSearch[]
  trace: TraceEvent[]
}

export type AgentStreamError = {
  code: string
  message: string
}
