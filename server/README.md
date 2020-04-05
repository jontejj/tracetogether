# Install
* kubectl
* minikube
* skaffold
 1. curl -Lo skaffold https://storage.googleapis.com/skaffold/releases/v1.0.0/skaffold-darwin-amd64 && chmod +x skaffold && sudo mv skaffold /usr/local/bin
* docker mmm
 1. Install Docker Desktop
 2. brew install docker-credential-helper

# Create cluster in gcloud
  1. gcloud beta container --project "tracetogether-273112" clusters create "cluster-1" --zone "europe-north1-a" --no-enable-basic-auth --release-channel "regular" --machine-type "n1-standard-1" --image-type "COS" --disk-type "pd-standard" --disk-size "100" --metadata disable-legacy-endpoints=true --scopes "https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" --num-nodes "3" --enable-stackdriver-kubernetes --enable-ip-alias --network "projects/tracetogether-273112/global/networks/default" --subnetwork "projects/tracetogether-273112/regions/europe-north1/subnetworks/default" --default-max-pods-per-node "110" --no-enable-master-authorized-networks --addons HorizontalPodAutoscaling,HttpLoadBalancing --enable-autoupgrade --enable-autorepair

# Deploy on GKE

1. Create a static IP address for the API:

NOTE: if you want to use a different name for the ip-address 
change that name in [ingress-patch.yaml](k8s/overlays/gke/ingress-patch.yaml)

```
gcloud --project "trace-together" compute addresses create trace-together-ip --global
gcloud --project "trace-together" compute addresses describe trace-together-ip --global 
```
1. Place firebase credentials here [k8s/base/conf/firebase-credentials.json](k8s/base/conf/).
2. Make sure [k8s/base/conf/firebase-config.json](k8s/base/conf/firebase-config.json) is configured correctly.
3. `skaffold deploy --default-repo <myrepo>` <myrepo> should be a repo that both you as user and the clusters has access to (Or [configure default repo](https://skaffold.dev/docs/environment/image-registries/))
