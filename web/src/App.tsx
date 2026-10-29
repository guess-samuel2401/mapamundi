// La aplicación: cada vez que se abre, el idioma; la primera vez, una partida de práctica; después, con qué
// continentes se juega y con qué dificultad, y a jugar (como Ventana.java de la versión sencilla de Windows).
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { usarIdioma, t, type Idioma } from './i18n'
import { nombreModo, type Atlas, type Continente, type Modo } from './modelo'
import { PartidaClasica } from './partida'
import { usarDaltonico } from './pintor'
import { cargarPreferencias, guardarPreferencias, type Preferencias } from './preferencias'
import { Sonidos } from './sonidos'
import { pantallaCompletaHorizontal } from './movil'
import { zona as nombreZona } from './comunes'
import { PantallaIdioma } from './pantallas/Idioma'
import { PantallaContinentes } from './pantallas/Continentes'
import { PantallaRetos } from './pantallas/Retos'
import { PantallaJuego } from './pantallas/Juego'
import { PantallaFin, type Resumen } from './pantallas/Fin'

type Pantalla = 'idioma' | 'continentes' | 'retos' | 'juego' | 'fin'

/** Los tres países fáciles de la partida de práctica. */
const PRACTICA = ['BR', 'EG', 'AU']

export function App({ atlas, base }: { atlas: Atlas; base: string }) {
  const [prefs, setPrefs] = useState<Preferencias>(cargarPreferencias)
  const sonidos = useMemo(() => new Sonidos(base), [base])
  const [pantalla, setPantalla] = useState<Pantalla>('idioma')
  const [zona, setZona] = useState<Continente[]>([])
  const [modo, setModo] = useState<Modo>('MAPA_PAIS')
  const [partida, setPartida] = useState<PartidaClasica | null>(null)
  /** Cambia con cada partida nueva, para que la pantalla de juego empiece de cero. */
  const [numeroPartida, setNumeroPartida] = useState(0)
  const [practica, setPractica] = useState(false)
  const [resumen, setResumen] = useState<Resumen | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  // Las preferencias se aplican y se guardan al momento.
  useEffect(() => {
    guardarPreferencias(prefs)
    usarDaltonico(prefs.daltonico)
    document.body.classList.toggle('daltonico', prefs.daltonico)
    sonidos.musicaActiva = prefs.musica
    sonidos.efectosActivos = prefs.efectos
    sonidos.aplicar()
  }, [prefs, sonidos])

  // El audio del teléfono se desbloquea con el primer toque (o tecla) del jugador, y se reanuda con cada uno.
  useEffect(() => {
    const desbloquear = () => sonidos.desbloquear()
    window.addEventListener('pointerdown', desbloquear)
    window.addEventListener('keydown', desbloquear)
    return () => {
      window.removeEventListener('pointerdown', desbloquear)
      window.removeEventListener('keydown', desbloquear)
    }
  }, [sonidos])

  const alternarMusica = useCallback(() => setPrefs((p) => ({ ...p, musica: !p.musica })), [])

  const empezar = useCallback(
    (p: PartidaClasica, esPractica: boolean) => {
      sonidos.musica('juego')
      setPartida(p)
      setNumeroPartida((n) => n + 1)
      setPractica(esPractica)
      setPantalla('juego')
    },
    [sonidos],
  )

  const practicar = useCallback(() => {
    const paises = PRACTICA.map((iso) => atlas.pais(iso)!).filter(Boolean)
    const continentes = [...new Set(paises.map((p) => p.continente))]
    setPrefs((p) => ({ ...p, practicaHecha: true }))
    empezar(new PartidaClasica({ modo: 'MAPA_PAIS', continentes }, paises), true)
  }, [atlas, empezar])

  const irAContinentes = useCallback(
    (mensaje: string | null = null) => {
      sonidos.musica('menu')
      setAviso(mensaje)
      setPantalla('continentes')
    },
    [sonidos],
  )

  const elegirIdioma = useCallback(
    (idioma: Idioma) => {
      usarIdioma(idioma)
      document.title = 'Mapamundi'
      setPrefs((p) => ({ ...p, idioma }))
      // El primer toque del jugador: desde aquí el navegador deja sonar la música y, en el teléfono, poner
      // la pantalla completa en horizontal.
      sonidos.desbloquear()
      void pantallaCompletaHorizontal()
      sonidos.musica('menu')
      if (!prefs.practicaHecha) practicar()
      else irAContinentes()
    },
    [sonidos, prefs.practicaHecha, practicar, irAContinentes],
  )

  const jugar = useCallback(
    (m: Modo, z: Continente[] = zona) => {
      setModo(m)
      empezar(new PartidaClasica({ modo: m, continentes: z }, atlas.paises), false)
    },
    [atlas, zona, empezar],
  )

  const terminar = useCallback(
    (p: PartidaClasica) => {
      if (practica) {
        irAContinentes(t('practica.fin'))
        return
      }
      sonidos.musica('menu')
      setResumen({
        puntaje: p.puntaje,
        ubicados: p.ubicados.size,
        ganada: p.ganada,
        nivelRecord: `${nombreZona([...p.enJuego])} · ${nombreModo(p.ajustes.modo)}`,
      })
      setPantalla('fin')
    },
    [practica, sonidos, irAContinentes],
  )

  // El botón Atrás del teléfono (y del navegador): en vez de salir de la página, vuelve a la pantalla anterior;
  // durante la partida, la pausa. Cada pantalla deja una entrada en el historial para poder volver de ella.
  const actual = useRef({ pantalla, partida, irAContinentes })
  actual.current = { pantalla, partida, irAContinentes }
  useEffect(() => {
    history.pushState({ pantalla }, '')
  }, [pantalla])
  useEffect(() => {
    const atras = () => {
      const { pantalla: p, partida: j, irAContinentes: volver } = actual.current
      if (p === 'juego') {
        if (j && !j.pausa && j.fase !== 'RESULTADO') j.alternarPausa()
        history.pushState({ pantalla: p }, '')
      } else if (p === 'retos' || p === 'fin') volver()
      else if (p === 'continentes') setPantalla('idioma')
      else history.pushState({ pantalla: p }, '')
    }
    window.addEventListener('popstate', atras)
    return () => window.removeEventListener('popstate', atras)
  }, [])

  let contenido
  switch (pantalla) {
    case 'idioma':
      contenido = (
        <PantallaIdioma
          inicial={prefs.idioma}
          daltonico={prefs.daltonico}
          onDaltonico={(d) => setPrefs((p) => ({ ...p, daltonico: d }))}
          onElegir={elegirIdioma}
        />
      )
      break
    case 'continentes':
      contenido = (
        <PantallaContinentes
          atlas={atlas}
          musica={prefs.musica}
          onMusica={alternarMusica}
          onAprender={practicar}
          onContinuar={(z) => {
            setZona(z)
            setAviso(null)
            setPantalla('retos')
          }}
        />
      )
      break
    case 'retos':
      contenido = (
        <PantallaRetos
          atlas={atlas}
          zona={zona}
          musica={prefs.musica}
          onMusica={alternarMusica}
          onElegir={(m) => jugar(m)}
          onVolver={() => irAContinentes()}
          onAprender={practicar}
        />
      )
      break
    case 'juego':
      contenido = partida && (
        <PantallaJuego
          key={numeroPartida}
          atlas={atlas}
          partida={partida}
          prefs={prefs}
          sonidos={sonidos}
          practica={practica}
          onPrefs={setPrefs}
          onSalir={() => irAContinentes()}
          onFin={terminar}
        />
      )
      break
    case 'fin':
      contenido = resumen && <PantallaFin resumen={resumen} onOtraVez={() => jugar(modo)} onCambiar={() => irAContinentes()} />
      break
  }

  return (
    <>
      {contenido}
      {aviso && pantalla === 'continentes' && (
        <div className="aviso" onClick={() => setAviso(null)}>
          {aviso}
        </div>
      )}
      <div className="girar">
        <div className="girar-icono">📱↻</div>
        <p className="titulo-claro">{t('web.girar')}</p>
        <p className="ayuda-clara">{t('web.girar.detalle')}</p>
      </div>
    </>
  )
}
