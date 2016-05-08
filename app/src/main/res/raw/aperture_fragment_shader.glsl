/*
 * Copyright (C) 2015 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

precision mediump float;

varying vec2 vTextureCoord;
uniform sampler2D sTexture;
uniform sampler2D sTexture2;
uniform float radius;
const float border = 0.03;

bool in_circle(vec2 p, vec2 c, float r) {
  float dx = (c.x - p.x);
  float dy = (c.y - p.y);
  dx *= dx;
  dy *= dy;
  return (dx + dy) <= (r * r);
}

void main() {
    vec4 tex1 = texture2D(sTexture, vTextureCoord);
    vec4 tex2 = texture2D(sTexture2, vTextureCoord);
    vec2 center = vec2(0.5, 0.5);
    vec2 uv = vTextureCoord;
    bool inCircle = in_circle(uv, center, radius);
    bool inCircleWithoutBorder = in_circle(uv, center, radius - border);
    if (inCircle && !inCircleWithoutBorder) {
        uv -= center;
        float dist =  sqrt(dot(uv, uv));
        float t = 1.0 + smoothstep(radius, radius+border, dist)
                - smoothstep(radius-border, radius, dist);
        gl_FragColor = mix(tex1, tex2, t);
    } else if (inCircle) {
        gl_FragColor = tex2;
    } else {
        gl_FragColor = tex1;
    }
}
