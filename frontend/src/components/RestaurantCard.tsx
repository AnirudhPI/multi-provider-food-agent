import type { Restaurant } from '../api/types'

type Props = {
  restaurant: Restaurant
  provider: string
}

export function RestaurantCard({ restaurant, provider }: Props) {
  return (
    <article className="restaurant-card">
      <div className="restaurant-image-wrap">
        {restaurant.imageUrl ? (
          <img className="restaurant-image" src={restaurant.imageUrl} alt="" loading="lazy" />
        ) : (
          <div className="restaurant-placeholder" aria-hidden="true">◌</div>
        )}
        <span className="provider-badge">{provider}</span>
      </div>
      <div className="restaurant-body">
        <div className="restaurant-heading">
          <div>
            <h3>{restaurant.name}</h3>
            <p>{restaurant.cuisines.length ? restaurant.cuisines.join(' · ') : 'Cuisine not listed'}</p>
          </div>
          {restaurant.rating !== null && <span className="rating">★ {restaurant.rating.toFixed(1)}</span>}
        </div>
        <div className="restaurant-meta">
          {restaurant.deliveryTimeMinutes !== null && <span>{restaurant.deliveryTimeMinutes} min</span>}
          {restaurant.areaName && <span>{restaurant.areaName}</span>}
          {restaurant.costForTwoDisplay && <span>{restaurant.costForTwoDisplay}</span>}
        </div>
        {restaurant.offer && <p className="offer">{restaurant.offer}</p>}
        <div className="restaurant-footnote">
          <span>{restaurant.totalRatingsDisplay ? `${restaurant.totalRatingsDisplay} ratings` : 'Live provider result'}</span>
          {restaurant.vegetarian === true && <span className="veg-mark">Vegetarian</span>}
        </div>
      </div>
    </article>
  )
}
