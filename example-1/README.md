# Install
```
$ helm --debug upgrade --install prepare-namespace ./prepare-namespace/ --namespace example-1 --create-namespace
$ helm --debug upgrade --install amq-keycloak-auth-example ./amq-keycloak-auth-example/ --namespace example-1
```

# Test
```
$ oc port-forward svc/postgresql 5432
```
## Publish a message
```
$ cd produce
$ mvn clean install
```
deploy the jar in the namespace

## Consume a message
```
$ cd consume
$ mvn clean install
```
deploy the jar in the namespace
