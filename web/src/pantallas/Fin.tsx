// Fin de la partida, sencillo: un mensaje según cómo fue, el puntaje en grande, los países ubicados y dos botones.
// Solo si el puntaje entra en el top 10 se pide el nombre.
import { useState } from 'react'
import { entero, t } from '../i18n'
import { entraEnLaTabla, guardarRecord } from '../preferencias'

export interface Resumen {
  puntaje: number
  ubicados: number
  ganada: boolean
  /** Lo que se guarda como «nivel» en la tabla de récords (zona · modo). */
  nivelRecord: string
}

function mensaje(r: Resumen): [string, string] {
  if (r.ganada) return ['🏆', t('clasico.fin.ganada')]
  if (r.ubicados === 0) return ['💪', t('fin.mensaje.0')]
  if (r.ubicados < 5) return ['🙂', t('fin.mensaje.1')]
  if (r.ubicados < 15) return ['😃', t('fin.mensaje.2')]
  return ['🌟', t('fin.mensaje.3')]
}

export function PantallaFin({ resumen, onOtraVez, onCambiar }: { resumen: Resumen; onOtraVez: () => void; onCambiar: () => void }) {
  const [dibujo, titulo] = mensaje(resumen)
  const [pedir] = useState(() => entraEnLaTabla(resumen.puntaje))
  const [nombre, setNombre] = useState('')
  const [guardado, setGuardado] = useState(false)
  const guardar = () => {
    guardarRecord({ nombre: nombre.trim() || t('fin.anonimo'), puntaje: resumen.puntaje, nivel: resumen.nivelRecord, fecha: new Date().toISOString().slice(0, 10) })
    setGuardado(true)
  }
  return (
    <main className="pantalla pantalla-centrada">
      <div className="panel tarjeta-fin">
        <div className="fin-dibujo">{dibujo}</div>
        <h1 className="titulo-grande">{titulo}</h1>
        <div className="hud-clave">{t('fin.puntaje')}</div>
        <div className="fin-puntaje">{entero(resumen.puntaje)}</div>
        <p className="negrita">{t('fin.paisesUbicados', resumen.ubicados)}</p>
        {pedir &&
          (guardado ? (
            <p className="acierto">✓ {t('fin.guardado')}</p>
          ) : (
            <form
              className="pedir-nombre"
              onSubmit={(e) => {
                e.preventDefault()
                guardar()
              }}
            >
              <p className="acierto">🏆 {t('fin.nuevoRecord')}</p>
              <div className="fila-nombre">
                <input value={nombre} maxLength={20} placeholder={t('fin.tuNombre')} onChange={(e) => setNombre(e.target.value)} autoFocus />
                <button className="boton-principal" type="submit">
                  {t('fin.guardar')}
                </button>
              </div>
            </form>
          ))}
        <button className="boton-principal boton-continuar" onClick={onOtraVez}>
          ▶ {t('fin.otraVez')}
        </button>
        <button className="boton-secundario" onClick={onCambiar}>
          {t('fin.cambiar')}
        </button>
      </div>
    </main>
  )
}
