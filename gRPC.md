Define a fixed outer protobuf contract.
Keep only the stable stuff in typed fields: pluginId, requestId, sessionId, messageType, timestamp, auth/context, and routing metadata.
Put plugin-owned data into google.protobuf.Struct or Value.
Use Struct for object-like JSON. Use Value only when the field can be scalar, array, object, or null. Core should treat these as opaque and only forward them.
Model the stream as a small message envelope.
One bidi stream per plugin connection. Every message has an explicit kind, like REGISTER, HEARTBEAT, REQUEST, RESPONSE, ERROR, UNREGISTER. That keeps the channel dumb in the good way.
Make the plugin the connection owner.
Plugin opens the stream to core, registers itself, then keeps the stream alive with heartbeats. Core tracks lease expiry and drops dead plugins. No periodic HTTP spam pretending to be architecture.
Route user requests through the stream, not direct REST.
Core receives user traffic, picks the plugin stream, sends a request envelope, waits for the matching response by requestId, and forwards the result back to the client.
Add versioning and validation at the envelope boundary.
Version the outer schema separately from plugin payloads. Validate only envelope fields in core. Do not deserialize plugin payload into core domain types unless you enjoy future migrations as a hobby.
Decide the JSON contract for opaque fields now.
Freeze naming, null handling, and scalar encoding rules. If plugin payloads are passed as Struct, make sure the plugin and client agree on how numbers, arrays, and nested objects are represented.

A minimal rule: core owns transport and routing, plugin owns payload shape. That split keeps the design from becoming a landfill of “temporary” parsing code.

For static assets, keep the core route as `/static/{pluginId}/...` and stream the upstream response straight through the servlet response. Avoid buffering the whole asset in memory; forward headers and status, then copy the response body with a small fixed buffer.

