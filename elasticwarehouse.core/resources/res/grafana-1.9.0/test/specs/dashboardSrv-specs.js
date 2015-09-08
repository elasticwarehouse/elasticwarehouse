/*! grafana - v1.9.0 - 2014-12-02
 * Copyright (c) 2014 Torkel Ödegaard; Licensed Apache License */

define(["services/dashboard/dashboardSrv"],function(){describe("when creating new dashboard with defaults only",function(){var a;beforeEach(module("grafana.services")),beforeEach(inject(function(b){a=b.create({})})),it("should have title",function(){expect(a.title).to.be("No Title")}),it("should have default properties",function(){expect(a.rows.length).to.be(0),expect(a.nav.length).to.be(1)})}),describe("when getting next panel id",function(){var a;beforeEach(module("grafana.services")),beforeEach(inject(function(b){a=b.create({rows:[{panels:[{id:5}]}]})})),it("should return max id + 1",function(){expect(a.getNextPanelId()).to.be(6)})}),describe("row and panel manipulation",function(){var a;beforeEach(module("grafana.services")),beforeEach(inject(function(b){a=b.create({})})),it("row span should sum spans",function(){var b=a.rowSpan({panels:[{span:2},{span:3}]});expect(b).to.be(5)}),it("adding default should split span in half",function(){a.rows=[{panels:[{span:12,id:7}]}],a.add_panel({span:4},a.rows[0]),expect(a.rows[0].panels[0].span).to.be(6),expect(a.rows[0].panels[1].span).to.be(6),expect(a.rows[0].panels[1].id).to.be(8)}),it("duplicate panel should try to add it to same row",function(){var b={span:4,attr:"123",id:10};a.rows=[{panels:[b]}],a.duplicatePanel(b,a.rows[0]),expect(a.rows[0].panels[0].span).to.be(4),expect(a.rows[0].panels[1].span).to.be(4),expect(a.rows[0].panels[1].attr).to.be("123"),expect(a.rows[0].panels[1].id).to.be(11)}),it("duplicate should add row if there is no space left",function(){var b={span:12,attr:"123"};a.rows=[{panels:[b]}],a.duplicatePanel(b,a.rows[0]),expect(a.rows[0].panels[0].span).to.be(12),expect(a.rows[0].panels.length).to.be(1),expect(a.rows[1].panels[0].attr).to.be("123")})}),describe("when creating dashboard with editable false",function(){var a;beforeEach(module("grafana.services")),beforeEach(inject(function(b){a=b.create({editable:!1})})),it("should set editable false",function(){expect(a.editable).to.be(!1)})}),describe("when creating dashboard with old schema",function(){var a,b;beforeEach(module("grafana.services")),beforeEach(inject(function(c){a=c.create({services:{filter:{time:{from:"now-1d",to:"now"},list:[1]}},pulldowns:[{type:"filtering",enable:!0},{type:"annotations",enable:!0,annotations:[{name:"old"}]}],rows:[{panels:[{type:"graphite",legend:!0,aliasYAxis:{test:2},grid:{min:1,max:10}}]}]}),b=a.rows[0].panels[0]})),it("should have title",function(){expect(a.title).to.be("No Title")}),it("should have panel id",function(){expect(b.id).to.be(1)}),it("should move time and filtering list",function(){expect(a.time.from).to.be("now-1d"),expect(a.templating.list[0]).to.be(1)}),it("graphite panel should change name too graph",function(){expect(b.type).to.be("graph")}),it("update legend setting",function(){expect(b.legend.show).to.be(!0)}),it("update grid options",function(){expect(b.grid.leftMin).to.be(1),expect(b.grid.leftMax).to.be(10)}),it("move aliasYAxis to series override",function(){expect(b.seriesOverrides[0].alias).to.be("test"),expect(b.seriesOverrides[0].yaxis).to.be(2)}),it("should move pulldowns to new schema",function(){expect(a.templating.enable).to.be(!0),expect(a.annotations.enable).to.be(!0),expect(a.annotations.list[0].name).to.be("old")}),it("dashboard schema version should be set to latest",function(){expect(a.version).to.be(6)})}),describe("when creating dashboard model with missing list for annoations or templating",function(){var a;beforeEach(module("grafana.services")),beforeEach(inject(function(b){a=b.create({annotations:{enable:!0},templating:{enable:!0}})})),it("should add empty list",function(){expect(a.annotations.list.length).to.be(0),expect(a.templating.list.length).to.be(0)})})});