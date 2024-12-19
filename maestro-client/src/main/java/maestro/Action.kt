package maestro

sealed class Action {
  data class TapPoint(val point: Point) : Action()

  sealed class SwipePoint : Action() {
      data class WithDirection(
          val direction: SwipeDirection,
          val startPoint: Point
      ) : SwipePoint()

      data class WithEndPoint(
          val startPoint: Point,
          val endPoint: Point
      ) : SwipePoint()
  }

  data class MultipleSwipePoint(
      val direction: SwipeDirection,
      val points: List<Point>
  ) : Action()
}
