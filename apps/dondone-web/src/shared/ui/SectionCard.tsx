import { ReactNode } from "react";

type SectionCardProps = {
  title?: string;
  description?: string;
  aside?: ReactNode;
  children: ReactNode;
};

export function SectionCard({
  title,
  description,
  aside,
  children
}: SectionCardProps) {
  const hasHead = Boolean(title || description || aside);

  return (
    <article className="card section-card">
      {hasHead ? (
        <div className="section-head">
          <div>
            {title ? <h3 className="section-title">{title}</h3> : null}
            {description ? <p className="section-sub">{description}</p> : null}
          </div>
          {aside}
        </div>
      ) : null}
      {children}
    </article>
  );
}
