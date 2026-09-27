# Перевод фрагмента иностранного источника

**Источник:** Jain P., Sukumar R., Zhao Y., Sanchez-Silva R., Fisher N. Building Service Topology at Scale: Architecture, Challenges, and Lessons Learned // Netflix TechBlog. — 2026. — 13 July. — URL: https://netflixtechblog.com/building-service-topology-at-scale-architecture-challenges-and-lessons-learned-f4b792f3f0d8

**Почему выбран:** статья о том, как в микросервисной архитектуре крупнейшего стримингового сервиса строят карту зависимостей между сервисами и обрабатывают потоковые данные. Это прямо связано с темой практики.

**Объём фрагмента:** около 2500 знаков (разделы «Introduction» и «Streaming-First: Why Real-Time Matters»).

> Черновой перевод. Перед сдачей вычитай его и перепиши своими словами там, где нужно.

---

## Оригинал

**Introduction**

In our first post, we introduced the problem: engineers at Netflix needed a unified, real-time view of service dependencies to troubleshoot faster, understand blast radius, and navigate our distributed architecture. We described our multi-source approach, combining eBPF network flows, IPC metrics, and distributed tracing into physically separate graph layers that can be queried independently or merged into a comprehensive view.

That post explained what we built and why. This post is about how, the engineering reality of building this system at Netflix scale.

Here's the truth: the first version worked perfectly… in our local environment. Production was a different story. Kafka consumers fell behind. Instances ran out of memory. Some nodes received 100x the traffic of others. Garbage collection pauses consumed more CPU than actual business logic.

What you'll learn in this post isn't a success story, it's a learning journey. We'll walk through the architecture decisions that enabled scale, the production challenges that tested those decisions, the optimization methodology that guided us through, and the lessons that apply to any distributed system. Along the way, we'll share the innovations that made it possible to process millions of flow records per second, reconstruct topology at any point in time, and provide sub-second query responses, all while maintaining near real-time freshness.

**Streaming-First: Why Real-Time Matters**

Traditional service topology systems use batch processing, aggregating data hourly or daily, then storing complete snapshots. This approach works at a modest scale but has a fundamental problem: by the time you see the data, it's already old. During a production incident at 3am, an hour-old dependency map is archaeology, not observability.

Our key architectural decision was to build streaming-first. Instead of batch jobs that process historical data, we continuously ingest flow records from multi-region Kafka streams and IPC metrics as Server-Sent Events, process them through reactive pipelines with backpressure handling, and provide near real-time topology updates, typically within tens of minutes, compared to the hours-old or day-old data that batch processing approaches provide.

This wasn't just about freshness, it was essential for our use cases. Live events can't wait for the next hourly batch. Incident response needs current data. Change validation requires seeing immediate impact. The architecture had to support continuous updates while handling massive scale without falling behind.

---

## Перевод

**Введение**

В первой статье мы описали проблему: инженерам Netflix требовалось единое представление зависимостей между сервисами в реальном времени. Оно нужно, чтобы быстрее устранять неполадки, оценивать радиус поражения при сбоях и ориентироваться в нашей распределённой архитектуре. Мы рассказали о подходе, который объединяет несколько источников данных: сетевые потоки eBPF, метрики межпроцессного взаимодействия (IPC) и распределённую трассировку. Эти данные хранятся в физически разделённых слоях графа, которые можно запрашивать по отдельности или объединять в общую картину.

В той статье мы объяснили, что построили и зачем. Эта статья о том, как мы это сделали и с какими инженерными трудностями столкнулись при создании системы в масштабах Netflix.

Честно говоря, первая версия прекрасно работала… в локальном окружении. В промышленной эксплуатации всё оказалось иначе. Потребители Kafka не успевали обрабатывать сообщения. Экземплярам сервиса не хватало памяти. Некоторые узлы получали в сто раз больше трафика, чем другие. Паузы на сборку мусора отнимали больше процессорного времени, чем сама бизнес-логика.

Эта статья не история успеха, а рассказ о том, чему мы научились. Мы разберём архитектурные решения, которые позволили системе масштабироваться, проблемы эксплуатации, проверившие эти решения на прочность, методику оптимизации, которой мы следовали, и выводы, применимые к любой распределённой системе. Попутно мы расскажем о решениях, благодаря которым удалось обрабатывать миллионы записей о сетевых потоках в секунду, восстанавливать топологию на любой момент времени и отвечать на запросы быстрее чем за секунду, сохраняя при этом актуальность данных, близкую к реальному времени.

**Приоритет потоковой обработки: почему важна работа в реальном времени**

Традиционные системы построения топологии сервисов используют пакетную обработку: данные агрегируются раз в час или раз в сутки, после чего сохраняются полные снимки. При умеренном масштабе такой подход работает, но у него есть принципиальный недостаток: к тому моменту, как вы видите данные, они уже устарели. Во время аварии в три часа ночи карта зависимостей часовой давности — это уже археология, а не наблюдаемость.

Ключевым архитектурным решением стал отказ от пакетной обработки в пользу потоковой. Вместо пакетных заданий, обрабатывающих исторические данные, мы непрерывно принимаем записи о сетевых потоках из мультирегиональных потоков Kafka и метрики IPC в виде Server-Sent Events. Эти данные проходят через реактивные конвейеры с механизмом обратного давления (backpressure), и топология обновляется почти в реальном времени, как правило в пределах нескольких десятков минут. При пакетной обработке данные отстают на часы или даже сутки.

Дело было не только в актуальности: без этого не работали бы наши сценарии использования. Прямые трансляции не могут ждать следующего ежечасного пакета. Для реагирования на инциденты нужны текущие данные. Чтобы проверить последствия изменений, их влияние нужно видеть сразу. Архитектура должна была поддерживать непрерывные обновления и справляться с огромными объёмами данных, не отставая от них.

---

## Глоссарий к фрагменту

| Термин | Перевод |
|---|---|
| service dependencies | зависимости между сервисами |
| blast radius | радиус поражения (область влияния сбоя) |
| distributed tracing | распределённая трассировка |
| IPC (inter-process communication) | межпроцессное взаимодействие |
| batch processing | пакетная обработка |
| streaming-first | приоритет потоковой обработки |
| backpressure | обратное давление |
| observability | наблюдаемость |
| production incident | инцидент в промышленной эксплуатации |
