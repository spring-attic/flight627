/**
 * Create a new object using another object as a prototype 'proto'.
 * If 'addProps' is provided, then all the 'own' properties of 'addProps' are
 * copied onto the new object.
 *
 * @param Object proto
 * @param Object addProps
 * @return Object
 */
function extend(proto, addProps) {
	//console.log('extending proto: '+JSON.stringify(proto));
	var obj = Object.create(proto);
	if (addProps) {
		for (var p in addProps) {
			if (addProps.hasOwnProperty(p)) {
				obj[p] = addProps[p];
			}
		}
	}
	return obj;
}

module.exports = extend;