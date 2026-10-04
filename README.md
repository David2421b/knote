# Knote con MongoDB y MinIO

Aplicación Spring Boot para crear notas con imágenes. MongoDB conserva el texto y MinIO conserva las imágenes para que todas las réplicas de Knote puedan recuperarlas.

## Requisitos

- Docker Desktop iniciado con contenedores Linux.
- Docker Compose v2.
- Minikube y `kubectl` para ejecutar en Kubernetes.
- JDK 21 si se ejecuta Maven fuera de Docker.

## Ejecutar localmente

Desde la raíz del repositorio:

```powershell
docker compose up --build
```

Abre <http://localhost:3000>, crea una nota, sube una imagen y publícala para confirmar que se muestra. La consola de MinIO está en <http://localhost:9001>; las credenciales de desarrollo predeterminadas son `knoteadmin` y `KnoteDevSecret123!`.

Detén los contenedores con `Ctrl+C`. Para eliminarlos, redes y datos persistentes incluidos, usa `docker compose down -v`.

## Ejecutar en Minikube

Inicia Minikube y comprueba que el nodo esté `Ready`:

```powershell
minikube start --driver=docker
kubectl get nodes
```

Construye la imagen directamente en el entorno de Minikube. Así Kubernetes puede encontrar `knote:1.0.0` sin descargarla de un registro:

```powershell
minikube image build -t knote:1.0.0 .
```

Aplica MongoDB, MinIO, el Secret de credenciales y Knote:

```powershell
kubectl apply -f kube
kubectl get pods -w
```

Cuando Knote, MongoDB y MinIO estén `Running`, abre la aplicación:

```powershell
minikube service knote --url
```

Prueba crear una nota, subir una imagen y confirmar que se muestra. Para demostrar que las imágenes son compartidas por todas las réplicas, escala Knote y repite la comprobación:

```powershell
kubectl scale deployment/knote --replicas=3
kubectl get pods -l app=knote -w
```

Al terminar, elimina los recursos de este ejercicio con `kubectl delete -f kube`. Minikube permanece instalado y puede detenerse con `minikube stop`.

## Evidencias de entrega

Guarda capturas legibles que muestren:

1. Minikube activo y el nodo en estado `Ready` (`minikube status` y `kubectl get nodes`).
2. Los Pods de Knote, MongoDB y MinIO en estado `Running` y Knote abierto desde la URL del servicio Minikube.
3. Una nota con imagen visible en la app local iniciada con Docker Compose.
4. Una nota con imagen visible en la app dentro de Minikube, idealmente después de escalar Knote a tres réplicas.
5. El repositorio GitHub con los cambios y la imagen publicada en Docker Hub. Agrega ambos enlaces al entregar la tarea.

## Configuración

La aplicación acepta `MONGO_URL`, `PORT`, `MINIO_HOST`, `MINIO_PORT`, `MINIO_BUCKET`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` y `MINIO_RECONNECT_ENABLED` como variables de entorno. Los valores incluidos en `compose.yaml` y `kube/knote.yaml` son únicamente para desarrollo local; reemplázalos antes de cualquier uso compartido o despliegue público.
