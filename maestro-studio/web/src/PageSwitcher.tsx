import { AnimatePresence, motion } from "framer-motion"
import { ReactElement } from 'react';


const PageSwitcher = ({ children, banner }: {
  banner: ReactElement | null
  children: [ReactElement, ReactElement | null]
}) => {
  const animationDuration = 0.06
  return (
    <motion.div
      className="flex flex-col gap-2 w-full h-full"
    >
      {banner ? (
        <motion.div
          initial={{ opacity: 0, scale: .9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: animationDuration }}
        >
          {banner}
        </motion.div>
      ) : null}
      <motion.div
        className="w-full h-full relative rounded-lg drop-shadow-md border bg-white shadow-l overflow-clip"
        layout
        transition={{ duration: animationDuration }}
      >
        <motion.div
          className="absolute w-full h-full bg-white"
        >
          {children[0]}
        </motion.div>
        <AnimatePresence>
          {children[1] ? (
            <motion.div
              className="absolute w-full h-full bg-white"
              initial={{ opacity: 0, translateY: '20px' }}
              animate={{ opacity: 1, translateY: 0 }}
              exit={{ opacity: 0, translateY: '20px' }}
              transition={{ duration: animationDuration }}
            >
              {children[1]}
            </motion.div>
          ) : null}
        </AnimatePresence>
      </motion.div>
    </motion.div>
  )
}

export default PageSwitcher
