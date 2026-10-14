import { Link } from 'react-router'

const STEPS = [
  'Организатор открывает свободные слоты в своём календаре.',
  'Клиент выбирает удобное время.',
  'Звонок подтверждён и появляется в календаре.',
]

const BENEFITS = [
  'Запись в пару кликов.',
  'Клиент сам выбирает удобное время.',
  'Свободные и занятые слоты видны сразу.',
]

function HomePage() {
  return (
    <main className="page">
      <section className="hero">
        <h1 className="hero__title">Записывайтесь на звонки без переписки</h1>
        <p className="hero__subtitle">
          Организатор открывает свободные слоты, клиент выбирает удобное время.
        </p>
        <Link className="button" to="/booking">
          Записаться на звонок
        </Link>
      </section>

      <section className="section" aria-labelledby="how-it-works-title">
        <h2 id="how-it-works-title" className="section__title">
          Как это работает
        </h2>
        <ol className="steps">
          {STEPS.map((step, index) => (
            <li key={step} className="steps__item">
              <span className="steps__number" aria-hidden="true">
                {index + 1}
              </span>
              <span>{step}</span>
            </li>
          ))}
        </ol>
      </section>

      <section className="section" aria-labelledby="benefits-title">
        <h2 id="benefits-title" className="section__title">
          Преимущества
        </h2>
        <ul className="benefits">
          {BENEFITS.map((benefit) => (
            <li key={benefit} className="benefits__item">
              {benefit}
            </li>
          ))}
        </ul>
      </section>

      <footer className="footer">
        <span className="footer__brand">Календарь звонков</span>
      </footer>
    </main>
  )
}

export default HomePage
