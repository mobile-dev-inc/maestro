import React from 'react';
import { DeviceScreen, UIElement } from './models';

const ElementSearch = ({deviceScreen, selectedElement, onElementSelected, hoveredElement, onElementHovered}: {
  deviceScreen: DeviceScreen
  selectedElement: UIElement | null
  onElementSelected: (element: UIElement | null) => void
  hoveredElement: UIElement | null
  onElementHovered: (element: UIElement | null) => void
}) => {
  const sortedElements = deviceScreen.elements.sort((a, b) => {
    if (!a.text) return 1
    if (!b.text) return -1
    return a.text.localeCompare(b.text)
  })
  return (
    <div className="flex flex-col h-full">
      <div className="font-bold text-lg">UI Elements</div>
      <div className="overflow-scroll">
        {sortedElements.map(element => (
          <div>{element.text}</div>
        ))}
      </div>
    </div>
  )
}

export default ElementSearch
