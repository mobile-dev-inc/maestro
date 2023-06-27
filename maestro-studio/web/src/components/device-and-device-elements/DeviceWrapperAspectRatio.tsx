import { useEffect, useRef, useState, ReactNode } from "react";

interface AspectRatioContainerProps
  extends React.HTMLAttributes<HTMLDivElement> {
  aspectRatio: number;
  children: ReactNode;
}

const DeviceWrapperAspectRatio = ({
  aspectRatio,
  children,
  ...rest
}: AspectRatioContainerProps) => {
  const [width, setWidth] = useState<number>(0);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const updateWidth = () => {
      if (containerRef.current) {
        const { height } = containerRef.current.getBoundingClientRect();
        const newWidth = height * aspectRatio;
        setWidth(newWidth);
      }
    };
    // Calculate initial width
    updateWidth();
    // Update width on screen resize
    window.addEventListener("resize", updateWidth);
    return () => {
      window.removeEventListener("resize", updateWidth);
    };
  }, [aspectRatio]);

  return (
    <div ref={containerRef} className="relative flex-grow">
      <div
        className="h-full max-w-full mx-auto grid place-items-center"
        style={{ width }}
        {...rest}
      >
        {children}
      </div>
    </div>
  );
};

export default DeviceWrapperAspectRatio;
