/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.flight.objects.services;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.flight.objects.FlightObject;
import org.eclipse.flight.objects.Resource;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * @author Miles Parker
 */
public class ContentAssist extends Resource {

	public static class Proposal extends FlightObject {

		public static class Position extends FlightObject {

			Integer offset;

			Integer length;

			public Integer getOffset() {
				return offset;
			}

			public void setOffset(Integer offset) {
				this.offset = offset;
			}

			public Integer getLength() {
				return length;
			}

			public void setLength(Integer length) {
				this.length = length;
			}

			@Override
			protected void fromJson(JsonObject json) {
				offset = json.getInteger("offset");
				length = json.getInteger("length");
			}

			@Override
			protected void toJson(JsonObject json, boolean thin) {
				json.putNumber("offset", offset).putNumber("length", length);
			}
		}

		public static class Description extends FlightObject {

			public static class Icon extends FlightObject {
				String src;

				public String getSrc() {
					return src;
				}

				public void setSrc(String src) {
					this.src = src;
				}

				@Override
				protected void fromJson(JsonObject json) {
					src = json.getString("src");
				}

				@Override
				protected void toJson(JsonObject json, boolean thin) {
					json.putString("src", src);
				}
			}

			public static class Segment extends FlightObject {

				public static class Style extends FlightObject {

					String color;

					public String getColor() {
						return color;
					}

					public void setColor(String color) {
						this.color = color;
					}

					@Override
					protected void fromJson(JsonObject json) {
						color = json.getString("color");
					}

					@Override
					protected void toJson(JsonObject json, boolean thin) {
						json.putString("color", color);
					}

				}

				String value;

				Style style;

				public String getValue() {
					return value;
				}

				public void setValue(String value) {
					this.value = value;
				}

				public Style getStyle() {
					return style;
				}

				public void setStyle(Style style) {
					this.style = style;
				}

				@Override
				protected void fromJson(JsonObject json) {
					value = json.getString("value");
					style = new Style();
					style.fromJson(json.getObject("style"));
				}

				@Override
				protected void toJson(JsonObject json, boolean thin) {
					json.putString("value", value);
					if (style != null) {
						json.putObject("style", style.toJson());
					}
				}
			}

			Collection<Segment> segments = new ArrayList<Segment>();

			Icon icon;

			public Collection<Segment> getSegments() {
				return segments;
			}

			public Icon getIcon() {
				return icon;
			}

			public void setIcon(Icon icon) {
				this.icon = icon;
			}

			@Override
			protected void fromJson(JsonObject json) {
				icon = new Icon();
				icon.fromJson(json.getObject("icon"));
			}

			@Override
			protected void toJson(JsonObject json, boolean thin) {
				if (icon != null) {
					json.putObject("icon", icon.toJson());
				}
				toJsonArray(json, "segments", segments);
			}
		}

		String proposal;
		Description description;
		Collection<Position> positions = new ArrayList<Position>();
		String style;
		Boolean replace;

		public String getProposal() {
			return proposal;
		}

		public void setProposal(String proposal) {
			this.proposal = proposal;
		}

		public Description getDescription() {
			return description;
		}

		public void setDescription(Description description) {
			this.description = description;
		}

		public Collection<Position> getPositions() {
			return positions;
		}

		public String getStyle() {
			return style;
		}

		public void setStyle(String style) {
			this.style = style;
		}

		public Boolean getReplace() {
			return replace;
		}

		public void setReplace(Boolean replace) {
			this.replace = replace;
		}

		@Override
		protected void fromJson(JsonObject json) {
			proposal = json.getString("proposal");
			style = json.getString("style");
			replace = json.getBoolean("replace");
			// not handling arrays yet
		}

		@Override
		protected void toJson(JsonObject json, boolean thin) {
			json.putString("proposal", proposal).putString("style", style)
					.putBoolean("replace", replace);
			toJsonArray(json, "positions", positions);
			if (description != null) {
				json.putObject("description", description.toJson());
			}
		}
	}

	Collection<Proposal> proposals;

	Integer offset;

	String prefix;

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * @return the proposals
	 */
	public Collection<Proposal> getProposals() {
		if (proposals == null) {
			proposals = new ArrayList<Proposal>();
		}
		return proposals;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.flight.resources.Resource#toJson(org.vertx.java.core.json
	 * .JsonObject, boolean)
	 */
	@Override
	protected void toJson(JsonObject json, boolean thin) {
		super.toJson(json, thin);
		json.putNumber("offset", offset).putString("prefix", prefix);
		toJsonArray(json, "proposals", getProposals());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.flight.resources.Resource#fromJson(org.vertx.java.core.json
	 * .JsonObject)
	 */
	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		offset = json.getInteger("offset");
		prefix = json.getString("prefix");
		// Don't need to handle arrays yet..
	}
}
