import { AnnotatedScreenshot } from './AnnotatedScreenshot';
import React, { useState } from 'react';
import { DeviceScreen, UIElement } from './models';
import PageSwitcher from './PageSwitcher';
import Banner from './Banner';
import ElementSearch from './ElementSearch';
import { motion } from 'framer-motion';
import Examples from './Examples';

const Footer = ({selectedElement, hoveredElement}: {
  selectedElement: UIElement | null
  hoveredElement: UIElement | null
}) => {
  const content = selectedElement ? (
    <span>asdf</span>
  ) : null
  return (
    <motion.div
      className="flex items-center px-3 bg-slate-600 h-8 text-white"
      initial={{ translateY: '40px' }}
      animate={{ translateY: 0 }}
      transition={{ ease: 'easeOut', duration: .05 }}
    >
      {content}
    </motion.div>
  )
}

const Inspect = ({ deviceScreen }: {
  deviceScreen: DeviceScreen
}) => {
  const [hoveredElement, setHoveredElement] = useState<UIElement | null>(null)
  const [selectedElement, setSelectedElement] = useState<UIElement | null>(null)
  
  const banner = selectedElement ? (
    <Banner
      left={selectedElement.text}
      right={selectedElement.resourceId}
      onClose={() => setSelectedElement(null)}
    />
  ) : null

  const detailsPage = selectedElement ? (
    <Examples element={selectedElement}/>
  ) : null;
  
  return (
    <motion.div
      className="flex flex-col overflow-hidden"
    >
      <motion.div
        initial={{ scale: .97, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ ease: 'easeOut', duration: .1 }}
        className="flex gap-10 p-10 overflow-hidden h-full"
      >
        <AnnotatedScreenshot
          deviceScreen={deviceScreen}
          onElementHovered={setHoveredElement}
          hoveredElement={hoveredElement}
          onElementSelected={setSelectedElement}
          selectedElement={selectedElement}
        />
        <PageSwitcher banner={banner}>
          <ElementSearch
            deviceScreen={deviceScreen}
            onElementHovered={setHoveredElement}
            hoveredElement={hoveredElement}
            onElementSelected={setSelectedElement}
          />
          {detailsPage}
        </PageSwitcher>
      </motion.div>
      <Footer
        selectedElement={selectedElement}
        hoveredElement={hoveredElement}
      />
    </motion.div>
  )
}

export default Inspect