function startsWith(str, pre) {
	return str.lastIndexOf(pre, 0) === 0;
}
function endsWith(str, suffix) {
	return str.indexOf(suffix, str.length - suffix.length) !== -1;
}
function toCompareString(obj) {
	return JSON.stringify(obj, null, '  ');
}

exports.startsWith = startsWith;
exports.endsWith = endsWith;
exports.toCompareString = toCompareString;