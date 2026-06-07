package ge.becrin.pouwer.challenge.api

const val REST_VALUE: String = "rest"
const val GRPC_VALUE: String = "grpc"

enum class PluginTransportMode(val value: String) {
    REST(REST_VALUE),
    GRPC(GRPC_VALUE)
}
