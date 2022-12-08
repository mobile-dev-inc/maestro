import { motion } from 'framer-motion';
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
  onElementSelected:  React.Dispatch<React.SetStateAction<UIElement | null>>
  hoveredElement: UIElement | null
  onElementHovered: React.Dispatch<React.SetStateAction<UIElement | null>>
}) => {
  const sortedElements = deviceScreen.elements.sort((a, b) => {
    return compare(a.text, b.text) || compare(a.resourceId, b.resourceId)
  })
  return (
    <div className="flex flex-col h-full">
      <div className="font-bold text-lg">UI Elements</div>
      <div className="flex flex-col overflow-scroll ">
        {sortedElements.map(element => (
          <div
            className="pb-1"
            key={element.id}
            onClick={() => {
              onElementSelected(element)
            }}
            onMouseOver={() => {
              onElementHovered(element)
            }}
            onMouseLeave={() => {
              onElementHovered(prev => prev === element ? null : prev)
            }}
          >
            <div
              className={`flex gap-3 p-5 items-center rounded border ${hoveredElement === element ? 'bg-slate-100' : ''} active:bg-slate-200`}
            >
              <span className="whitespace-nowrap overflow-hidden text-ellipsis cursor-default">{element.text}</span>
              <div className="flex-1"/>
              <span className="whitespace-nowrap overflow-hidden text-ellipsis cursor-default">{element.resourceId}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default ElementSearch
