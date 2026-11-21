import { expect, test } from '@playwright/test'

test('completes the Swiggy discovery interface flow', async ({ page, context }) => {
  await context.grantPermissions(['geolocation'])
  await context.setGeolocation({ latitude: 12.9716, longitude: 77.5946 })
  await page.route('**/api/v1/agent/turns/stream', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: [
        'event:trace\ndata:{"turnId":"e2e","type":"TOOL_STARTED","step":1,"toolName":"search_restaurants","occurredAt":"2026-11-20T10:00:00Z"}\n\n',
        'event:result\ndata:{"turnId":"e2e","outcome":"COMPLETED","message":"Here is a grounded result from Swiggy.","restaurantSearches":[{"provider":"SWIGGY","query":"dosa","restaurants":[{"providerRestaurantId":"one","name":"Learning Dosa House","cuisines":["South Indian"],"rating":4.4,"totalRatingsDisplay":"1K+","costForTwoDisplay":"₹250 for two","areaName":"Central","distanceKm":2.1,"deliveryTimeMinutes":25,"vegetarian":true,"offer":"20% OFF","imageUrl":null,"availabilityStatus":"OPEN"}],"nextContinuationToken":null,"hasMore":false,"observedAt":"2026-11-20T10:00:01Z"}],"trace":[]}\n\n',
      ].join(''),
    })
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Use my location' }).click()
  await expect(page.getByText('Location ready')).toBeVisible()
  await page.getByLabel('What food would you like to find?').fill('Find vegetarian dosa')
  await page.getByRole('button', { name: 'Send request' }).click()

  await expect(page.getByText('Learning Dosa House')).toBeVisible()
  await expect(page.getByText('Here is a grounded result from Swiggy.')).toBeVisible()
  await expect(page.getByText('20% OFF')).toBeVisible()
})
