import React from 'react';

export type HTMLProps<T> = React.DetailedHTMLProps<React.HTMLAttributes<T>, T>
export type DivProps = HTMLProps<HTMLDivElement>

export type UIElementBounds = {
  x: number
  y: number
  width: number
  height: number
}

export type UIElement = {
  id: string
  bounds: UIElementBounds | null
  resourceId: string | null
  text: string | null
}

export type Hierarchy = {
  screenshot: string
  elements: UIElement[]
}