import React from 'react';
import { DeviceScreen, UIElement } from './models';

const compare = (a: string | undefined, b: string | undefined) => {
  if (!a) return b ? 1 : 0
  if (!b) return -1
  return a.localeCompare(b)
}

const ElementSearch = ({deviceScreen, selectedElement, onElementSelected, hoveredElement, onElementHovered}: {
  deviceScreen: DeviceScreen
  selectedElement: UIElement | null
  onElementSelected: (element: UIElement | null) => void
  hoveredElement: UIElement | null
  onElementHovered: (element: UIElement | null) => void
}) => {
  const sortedElements = deviceScreen.elements.sort((a, b) => {
    return compare(a.text, b.text) || compare(a.resourceId, b.resourceId)
  })
  return (
    <div className="flex flex-col h-full">
      <div className="font-bold text-lg">UI Elements</div>
      <div className="overflow-scroll">
        {sortedElements.map(element => (
          <div className="flex gap-3 items-center p-5 rounded border border overflow-hidden">
            <span className="whitespace-nowrap overflow-hidden text-ellipsis">{element.text}</span>
            <div className="flex-1"/>
            <span className="whitespace-nowrap overflow-hidden  text-ellipsis">{element.resourceId}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default ElementSearch
