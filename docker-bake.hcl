// Multi-platform image builds: `docker buildx bake` (prod) or `docker buildx bake dev`.
// amd64 + arm64 so Apple Silicon dev machines and x86 clusters run the same tag.

group "default" {
  targets = ["prod"]
}

target "prod" {
  dockerfile = "Dockerfile"
  target     = "prod"
  platforms  = ["linux/amd64", "linux/arm64"]
  tags       = ["breuninger/homefeed:prod"]
}

target "dev" {
  dockerfile = "Dockerfile"
  target     = "dev"
  // dev image only ever runs on the local machine - multi-arch buys nothing here
  tags       = ["breuninger/homefeed:dev"]
}
