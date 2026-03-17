import { ReactNode } from "react";

type SectionCardProps = {
  title: string;
  description: string;
  aside?: ReactNode;
  children: ReactNode;
};

export function SectionCard({
  title,
  description,
  aside,
  children
}: SectionCardProps) {
  return (
    <article className="card section-card">
      <div className="section-head">
        <div>
          <h3 className="section-title">{title}</h3>
          <p className="section-sub">{description}</p>
        </div>
        {aside}
      </div>
      {children}
    </article>
  );
}
