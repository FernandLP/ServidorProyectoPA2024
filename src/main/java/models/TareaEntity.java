package models;

import jakarta.persistence.*;

import java.sql.Date;

@Entity
@Table(name = "tarea", schema = "gestion_tareas", catalog = "")
public class TareaEntity {
    private int id;
    private String titulo;
    private String descripcion;
    private Date fechaLimite;
    private String prioridad;
    private String color;
    private Integer usuarioId;

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "titulo", nullable = false, length = 255)
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    @Basic
    @Column(name = "descripcion", nullable = true, length = -1)
    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Basic
    @Column(name = "fecha_limite", nullable = true)
    public Date getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(Date fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    @Basic
    @Column(name = "prioridad", nullable = true, length = 50)
    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    @Basic
    @Column(name = "color", nullable = true, length = 50)
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Basic
    @Column(name = "usuario_id", nullable = true)
    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TareaEntity that = (TareaEntity) o;

        if (id != that.id) return false;
        if (titulo != null ? !titulo.equals(that.titulo) : that.titulo != null) return false;
        if (descripcion != null ? !descripcion.equals(that.descripcion) : that.descripcion != null) return false;
        if (fechaLimite != null ? !fechaLimite.equals(that.fechaLimite) : that.fechaLimite != null) return false;
        if (prioridad != null ? !prioridad.equals(that.prioridad) : that.prioridad != null) return false;
        if (color != null ? !color.equals(that.color) : that.color != null) return false;
        if (usuarioId != null ? !usuarioId.equals(that.usuarioId) : that.usuarioId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (titulo != null ? titulo.hashCode() : 0);
        result = 31 * result + (descripcion != null ? descripcion.hashCode() : 0);
        result = 31 * result + (fechaLimite != null ? fechaLimite.hashCode() : 0);
        result = 31 * result + (prioridad != null ? prioridad.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (usuarioId != null ? usuarioId.hashCode() : 0);
        return result;
    }
}
