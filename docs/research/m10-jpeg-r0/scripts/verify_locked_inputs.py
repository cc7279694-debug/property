"""Read-only fixture/geometry preflight. This is NOT a JPEG decoder."""
from pathlib import Path
import argparse
import hashlib
import json
import math


def geometry(data):
    assert data[:2] == b'\xff\xd8'
    i = 2
    while i < len(data):
        assert data[i] == 255
        while data[i] == 255:
            i += 1
        marker = data[i]
        i += 1
        if marker in (0xD9, 0xDA):
            break
        length = int.from_bytes(data[i:i + 2], 'big')
        assert length >= 2 and i + length <= len(data)
        if marker in (0xC0, 0xC2):
            return {'sofMarker': hex(marker), 'precision': data[i + 2],
                    'height': int.from_bytes(data[i + 3:i + 5], 'big'),
                    'width': int.from_bytes(data[i + 5:i + 7], 'big'),
                    'components': data[i + 7]}
        i += length
    raise AssertionError('No supported SOF metadata found; no entropy decoding performed')


parser = argparse.ArgumentParser()
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[4]
source = root / 'app/src/androidTest/assets/m10_jpeg_integrity'
expected = {
    'complete.jpg': (59334, '2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907'),
    'entropy_cut_eoi_retained.jpg': (20194, 'f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a'),
}
rows = []
buffers = {}
for name, (size, sha) in expected.items():
    b = (source / name).read_bytes()
    assert len(b) == size and hashlib.sha256(b).hexdigest() == sha
    buffers[name] = b
    rows.append({'file': (source / name).relative_to(root).as_posix(), 'bytes': size,
                 'sha256': sha, 'geometryOnly': geometry(b), 'hasEoi': b[-2:] == b'\xff\xd9',
                 'strictDecodeActual': 'NOT_RUN', 'strictReturnCode': None,
                 'strictWarnings': None, 'strictErrors': None})
assert buffers['entropy_cut_eoi_retained.jpg'] == buffers['complete.jpg'][:20192] + buffers['complete.jpg'][59332:]
# Illustrative arithmetic only; these are not native allocation/RSS measurements.
w, h = 8000, 5000
y_blocks = math.ceil(w / 8) * (math.ceil(math.ceil(h / 8) / 2) * 2)
c_blocks = math.ceil(w / 16) * math.ceil(h / 16)
estimates = {'kind': 'CALCULATED_NOT_MEASURED', 'exampleWidth': w, 'exampleHeight': h,
             'fullRgbaBytes': w * h * 4,
             'progressive444CoefficientPayloadBytes': math.ceil(w / 8) * math.ceil(h / 8) * 3 * 64 * 2,
             'progressive420PaddedCoefficientPayloadBytes': (y_blocks + 2 * c_blocks) * 64 * 2,
             'excluded': 'Pointer tables, pools, marker data, compressed input, stack, allocator overhead, JNI, output and Android normalization; not a universal maximum.'}
out = {'kind': 'INPUT_AND_ARITHMETIC_PREFLIGHT_ONLY', 'result': 'PASS', 'lockedInputs': rows,
       'exactDeletion': {'fromInclusive': 20192, 'untilExclusive': 59332, 'removedBytes': 39140,
                         'remainingSuffixHex': 'ffd9'},
       'nativeDecodeStatus': 'BLOCKED_NOT_RUN', 'resourceEstimates': estimates,
       'noInputWritten': True, 'newFixturesGenerated': 0}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(out, indent=2) + '\n', encoding='utf8')
print(json.dumps(out, indent=2))
